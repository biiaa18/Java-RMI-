package com.ass1.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Request {
    private String methodName = null;
    private List<Object> argList = null;
    private Integer zoneNumber = null;
    private Class<?>[] argTypesList = null;

    public String getMethodName() {
        return methodName;
    }

    public List<Object> getArgList() {
        return argList;
    }

    public Integer getZoneNumber() {
        return zoneNumber;
    }

    public Class<?>[] getArgTypesList() {
        return argTypesList;
    }

    /// for making new request object, when reading request line
    public Request(String inputMethod, List<Object> argumentsList, Integer zoneN, Class<?>[] argTypes) {
        // Validate request parameters
        if (inputMethod == null || inputMethod.isEmpty()) {
            System.out.println("Error: Method name cannot be null or empty.");
            throw new IllegalArgumentException("Method name cannot be null or empty.");
        }
        if (argumentsList.getFirst() instanceof String firstArg) {
            if (firstArg.isEmpty()) {
                System.out.println("Error: First arg should not be empty if it's a string.");
                throw new IllegalArgumentException("First arg should not be empty if it's a string.");
            }
        }

        this.methodName = inputMethod;
        this.argList = argumentsList;
        this.zoneNumber = zoneN;
        this.argTypesList = argTypes;
    }

    /// read line and split it into arguments
    public static Request readLine(String line) {
        //split line by space first
        List<Object> requestArguments = new ArrayList<>(Arrays.asList(line.split(" ")));
        //always first
        String methodNamee = (String) requestArguments.remove(0);
        //always last
        Integer clientZoneNumber = Integer.parseInt(((String) requestArguments.removeLast()).split(":")[1]);
        //based on method, number of arguments change, but city can also be of several words
        //split arguments of the method correctly
        List<Object> arguments = splitArguments(methodNamee, requestArguments);
        //add zone number at the end for future delay calculation
        arguments.add(clientZoneNumber);
        Class<?>[] typesOfArg = defineArgTypes(arguments);
        return new Request(methodNamee, arguments, clientZoneNumber, typesOfArg);
    }

    /// need to match argument types with function we remotely call later, to find it
    private static Class<?>[] defineArgTypes(List<Object> arguments) {
        Class<?>[] typesOfArgg = new Class<?>[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i) instanceof Integer) {
                typesOfArgg[i] = Integer.class;
                continue;
            }
            String argument = (String) arguments.get(i);
            //check for possible negative int and find how many numbers
            //save type to array of types and change type in the argument list as well
            if (argument.matches("-?\\d+")) {
                typesOfArgg[i] = Integer.class;
                arguments.set(i, Integer.parseInt(argument));
            } else {
                typesOfArgg[i] = String.class;
            }
        }

        return typesOfArgg;
    }

    /// split arguments correctly (case: city of 2 words)
    //getPopulationofCountry : 1   // has name
    //getNumberofCities : 3        //has name
    //getNumberofCountries : 3
    //getNumberofCountriesMM : 3
    // TODO: Rework this to be dynamic, and so that it works for T when we add that later
    private static List<Object> splitArguments(String methodNamee, List<Object> line) {
        //case: no names
        if (methodNamee.equals("getNumberofCountriesMM") || methodNamee.equals("getNumberofCountries")) {
            return line;
        }

        List<Object> returnList = new ArrayList<>();
        //case: 1 argument: name
        if (methodNamee.equals("getPopulationofCountry")) {
            StringBuilder completeArg = new StringBuilder();
            for (int i = 0; i < line.size(); i++) {
                completeArg.append(line.get(i));
                //everything is a name, but dont add space at the end
                if (i < line.size() - 1) {
                    completeArg.append(" ");
                }

            }
            returnList.add(completeArg.toString());
            return returnList;
        }
        //case: 3 arguments, name
        else if (methodNamee.equals("getNumberofCities")) {
            StringBuilder completeArg = new StringBuilder();
            //name has spaces, 2 arg from the end are not name
            for (int i = 0; i < line.size() - 2; i++) {
                completeArg.append(line.get(i));
                //do not add space at the end of name
                if (i < line.size() - 3) {
                    completeArg.append(" ");
                }
            }
            returnList.add(completeArg.toString());
            returnList.add(line.get(line.size() - 2));
            returnList.add(line.get(line.size() - 1));
        } else {
            throw new IllegalArgumentException("method doesn't exist: " + methodNamee);
        }
        return returnList;
    }
}