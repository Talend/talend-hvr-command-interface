package com.talend.hvr;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talend.tmc.dom.Executable;
import com.talend.tmc.dom.Execution;
import com.talend.tmc.dom.ExecutionRequest;
import com.talend.tmc.dom.ExecutionResponse;
import com.talend.tmc.services.TalendBearerAuth;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.executables.ExecutableService;
import com.talend.tmc.services.executions.ExecutionService;
import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

/**
 * Created by tbennett on 11/5/16.
 * Update: 8/19/2017 - Change for Carbonite to add flag so ICS Jobs in a state of Warning does not return exit code of 1
 * Update: 8/19/2017 - Change for Teradata so they can update passwords via CLI in either bulk or single connection
 */
public class tcli {
    private static final Logger logger = Logger.getLogger(tcli.class);
    private static final int _EXITGOOD = 0;
    private static final int _EXITBAD = 1;
    private static final String _CHECKPOINTFILE = "tcli_hvr.checkpoint";
    private static ObjectMapper mapper;

    public static void main(String[] args) {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String workingDir = new File(".").getAbsolutePath();
        long last_checkpoint = -1;
        try {
            BufferedReader cprdr = new BufferedReader(new FileReader(new File(workingDir+File.separator+_CHECKPOINTFILE)));
            last_checkpoint = Long.parseLong(cprdr.readLine().trim().split("\\.")[0]);
            cprdr.close();
        } catch (IOException e)
        {
            logger.warn("Checkpoint file does not exist. Will create.");
        }



        logger.debug("Passing CLI arguement in to be parsed:: " + Arrays.toString(args));
        Cli.parse(args);
        logger.debug("CLI Arguments parsed successfully");
        logger.info("Getting sorted list of HVR Manifest Files from " + Cli.getCliValue("hm"));
        if (!new File(Cli.getCliValue("hm")).exists())
            printErrorMessage(Cli.getCliValue("hm") + " path does not exists! Please check value and try again.");
        File[] files = getHvrManifestFiles(Cli.getCliValue("hm"));

        String token = null;
        if (Cli.hasCliValue("t"))
            token = Cli.getCliValue("t");
        else if (Cli.hasCliValue("te"))
            token = System.getenv(Cli.getCliValue("te"));

        if (token == null) {
            printErrorMessage("Token not found. Please check command and try again.");
        }

        TalendCredentials credentials = new TalendBearerAuth(token);
        try {
            TalendManifest talendManifest = mapper.readValue(new File(Cli.getCliValue("tm")), TalendManifest.class);
            Hashtable<String, String> executedJobs = new Hashtable<>();
            ExecutionService executionService = ExecutionService.instance(credentials, TalendCloudRegion.valueOf(Cli.getCliValue("r")));
            for (File file : files) {
                long hvrManifestCheckPoint = Long.parseLong(file.getName().split("\\.")[0]);
                while (hvrManifestCheckPoint > last_checkpoint) {
                    HVRManifest hvrManifest = mapper.readValue(file, HVRManifest.class);
                    for (String hvrTable : hvrManifest.getTables()) {
                        if (talendManifest.contains(hvrTable)) {
                            ExecutableService executableService = ExecutableService.instance(credentials, TalendCloudRegion.valueOf(Cli.getCliValue("r")));
                            SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");

                            String envName = Cli.hasCliValue("e") ? Cli.getCliValue("e") : "default";
                            Mapping mapping = talendManifest.getMapping(hvrTable);
                            String query = fiql.is("name").equalTo(mapping.getTalendJob()).and().is("workspace.environment.name").equalTo(envName).query();
                            Executable[] executables = executableService.getByQuery(query);

                            if (executables.length > 1)
                                printErrorMessage("More than 1 Job returned with that name!");

                            Hashtable<String, String> parameters = null;


                            ExecutionRequest executionRequest = new ExecutionRequest();
                            executionRequest.setExecutable(executables[0].getExecutable());
                            if (mapping.getParameters().size() > 0) {
                                String[] pairs = Cli.getCliValue("cv").split(";");
                                parameters = new Hashtable<>();
                                for (Parameter parameter : mapping.getParameters()) {
                                    Hashtable<String, String> contextVariables = parameter.getContextVariables();
                                    for (String key : contextVariables.keySet())
                                    {
                                        parameters.put(key, contextVariables.get(key));
                                    }
                                }

                                executionRequest.setParameters(parameters);
                            }

                            ExecutionResponse executionResponse = executionService.post(executionRequest);
                            printMessage("Talend Job Started: " + executionResponse.getExecutionId());
                            executedJobs.put(executionResponse.getExecutionId(), "STARTED");

                        }
                    }

                    if (Cli.hasCliValue("w")) {
                        boolean isActive = true;
                        while(isActive) {
                            executedJobs.forEach((key, value) -> {
                                try {
                                    Execution execution = executionService.get(key);
                                    if (execution.getFinishTimestamp() != null) {
                                        if (!execution.getExecutionStatus().equals("EXECUTION_SUCCESS"))
                                            printErrorMessage("Job Completed in non Successful State :" + execution.toString());
                                        else
                                            executedJobs.replace(key, "SUCCESS");
                                    }
                                } catch(TalendRestException | IOException ex)
                                {
                                    logger.warn(ex.getMessage());
                                }
                            });

                            isActive = false;
                            for (String key : executedJobs.keySet())
                            {
                                if (executedJobs.get(key) == null)
                                {
                                    isActive = true;
                                    break;
                                }
                            }


                            Thread.sleep(5000);
                        }
                    }
                    last_checkpoint = hvrManifestCheckPoint;
                }
            }

            updateCheckpointFile(last_checkpoint);
            System.exit(_EXITGOOD);
        } catch (TalendRestException | IOException | InterruptedException ex) {
            printErrorMessage(ex.getMessage());
        }
    }

    private static void printMessage(String message)
    {
        System.out.println(message);
    }

    private static void printErrorMessage(String message) {
        System.err.println(message);
        System.exit(_EXITBAD);
    }

    private static void updateCheckpointFile(long checkpoint)
    {
        try {
            BufferedWriter cpwrt = new BufferedWriter(new FileWriter(new File(_CHECKPOINTFILE)));
            cpwrt.write(checkpoint+".json");
            cpwrt.flush();
            cpwrt.close();
        } catch(IOException e)
        {
            printErrorMessage(e.getMessage());
        }
    }
    private static String stripNonValidCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in))) return ""; // vacancy test.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                    (current == 0xA) ||
                    (current == 0xD) ||
                    ((current >= 0x20) && (current <= 0xD7FF)) ||
                    ((current >= 0xE000) && (current <= 0xFFFD)) ||
                    ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }

    private static File[] getHvrManifestFiles(String path)
    {
        File[] files = new File(path).listFiles();

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        return files;
    }

}