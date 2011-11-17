import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EntryPoint {


    public static void main(String[] inArgs) {

        List<String> argsList = new ArrayList<String>();
        List<String> optionList = new ArrayList<String>();

        for(String inArg : inArgs){
            if(!inArg.startsWith("-")){
                argsList.add(inArg);
            }
            else{
                optionList.add(inArg);
            }
        }

        String[] args = new String[argsList.size()];
        args = argsList.toArray(args);


        if (args.length < 1) {
            System.out.println("USAGE: template [-option1|-option2] <template filename>=<output filename> <template2>=<output2>");
            System.out.println("   OR: template [-option1|-option2] <input file>");
            System.out.println("OPTIONS:");
            System.out.println("-debug      keep generated java files. for debugging");
            System.out.println("-refresh    overwrite output file only when template is newer than output");
            return;
        }

        boolean keepJavaFiles = false;

        if(optionList.contains("-debug")){
            keepJavaFiles = true;
        }

        boolean dontRewriteCurrent = false;
        if(optionList.contains("-refresh")){
            dontRewriteCurrent = true;
        }


        try{
            if(args.length == 1 && args[0].indexOf("=") < 0){
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
                String str = null;
                while((str = reader.readLine()) != null){
                    String[] toSplit = str.split("=");
                    if(toSplit.length != 2) continue;
                    ProcessOneTemplate(keepJavaFiles, toSplit[0], toSplit[1],dontRewriteCurrent);
                }
            }
            else{
                for(String templateOutput : args){
                    String[] toSplit = templateOutput.split("=");
                    if(toSplit.length != 2) continue;
                    ProcessOneTemplate(keepJavaFiles, toSplit[0], toSplit[1],dontRewriteCurrent);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void ProcessOneTemplate(boolean keepJavaFiles, String inputTemplateName, String outputTemplateName, boolean dontRewriteCurrent) {
        if(dontRewriteCurrent){
            File inFile = new File(inputTemplateName);
            File outFile = new File(outputTemplateName);
            if (outFile.exists() && inFile.lastModified() <= outFile.lastModified()) {
                return;
            }
        }
        TemplateProcessor templateProcessor = new TemplateProcessor(inputTemplateName, outputTemplateName);
        templateProcessor.setDeleteJavaFile(!keepJavaFiles);
        templateProcessor.run();
    }


}
