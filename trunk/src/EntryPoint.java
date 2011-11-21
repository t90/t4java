import sun.rmi.runtime.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EntryPoint {

    private static List<File> _dependencies = null;

    public static <T> List<T> where(List<T> source, Func<T, Boolean> selector){
        ArrayList<T> retVal = new ArrayList<T>();
        for(T item : source){
            if(selector.run(item)){
                retVal.add(item);
            }
        }
        return retVal;
    }

    public static <T,T1> List<T1> select(List<T> source, Func<T, T1> mapper){
        ArrayList<T1> retVal = new ArrayList<T1>();
        for(T item : source){
            retVal.add(mapper.run(item));
        }
        return retVal;
    }

    public static <T1,T2> List<T1> selectMany(List<T2> source, Func<T2, List<T1>> selector){
        ArrayList<T1> retVal = new ArrayList<T1>();

        for(T2 param : source){
            retVal.addAll(selector.run(param));
        }

        return retVal;
    }

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
            System.out.println("USAGE: template [-option1|-option2] <template filename>=<output filename> '<template2>=<output2> arg1 arg2");
            System.out.println("   OR: template [-option1|-option2] <input file>");
            System.out.println("OPTIONS:");
            System.out.println("-debug                  keep generated java files. for debugging");
            System.out.println("-refresh                overwrite output file only when template is newer than output");
            System.out.println("-extradeps:<filename>   for use with -refresh, check also files listed in `filename` to be newer than the output.");
            return;
        }

        boolean keepJavaFiles = false;

        if(optionList.contains("-debug")){
            keepJavaFiles = true;
        }

        List<String> extrDeps = null;


        boolean dontRewriteCurrent = false;
        if(optionList.contains("-refresh")){
            dontRewriteCurrent = true;

            extrDeps = where(optionList, new Func<String, Boolean>() {
                @Override
                public Boolean run(String param) {
                    return param.startsWith("-extradeps:");
                }
            });
            extrDeps = select(extrDeps,new Func<String, String>(){
                @Override
                public String run(String param) {
                    String[] split = param.split(":");
                    if(split.length < 2){
                        return "";
                    }
                    return split[1];
                }
            });

            _dependencies = selectMany(extrDeps,new Func<String, List<File>>() {
                @Override
                public List<File> run(String fileName) {
                    List<File> retVal = new ArrayList<File>();
                    try{
                        BufferedReader fReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
                        String str = null;
                        while((str = fReader.readLine()) != null){
                            str = str.trim();
                            if("".equals(str)) continue;
                            retVal.add(new File(str));
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    return retVal;
                }
            });
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
            final File outFile = new File(outputTemplateName);
            List<File> depenedncyCheck = _dependencies == null ? new ArrayList<File>() : _dependencies;
            depenedncyCheck.add(inFile);

            if(outFile.exists()){
                List<File> newerFiles = where(depenedncyCheck, new Func<File, Boolean>() {
                    @Override
                    public Boolean run(File param) {
                        if (param.lastModified() > outFile.lastModified()) {
                            return true;
                        }
                        return false;
                    }
                });
                if(newerFiles.size() == 0){
                    return;
                }
            }
        }
        TemplateProcessor templateProcessor = new TemplateProcessor(inputTemplateName, outputTemplateName);
        templateProcessor.setDeleteJavaFile(!keepJavaFiles);
        templateProcessor.run();
    }


}
