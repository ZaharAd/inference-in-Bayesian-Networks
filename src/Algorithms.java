import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by zahar on 05/01/17.
 */
public class Algorithms {

    private HashMap<String, Node> Vars;
    private HashMap<String, Variable> knownVariebles;
    private int mulCount;
    private int plusCount;
    private int FIRST_ELEMENT = 0;
    private IOfile file;

    /**
     * Constructor for Algorithms
     * @param _Vars
     * @param _file
     */
    public Algorithms(HashMap<String, Node> _Vars, IOfile _file) {
        Vars =_Vars;
        file  = _file;
        file.makeFileToWrite();
    }



    /**
     * Simple Inference
     * @param query
     */
    public void firstAlgo(myQuery query) {
        mulCount = 0;
        plusCount = 0;

        //numerator calculation
        double numeratorSum = calcQuery(query);

        //denominator calculation - the complementary of the query variable
        double denominatorSum = 0 ;

        ArrayList <Double> complementaryList = new ArrayList<>();
        String  []  queryVarValues = Vars.get(query.getQueryVariable().getVariableName()).getValues();

        //calculate for each value of the query variable
        for (String value : queryVarValues) {
            if (!value.substring(1).equals(query.getQueryVariable().getValue())) {

                //make new query with different value
                myQuery compQuery = new myQuery(
                        new Variable(query.getQueryVariable().getVariableName(), value.substring(1)),
                        query.getEvidenceVariables(), query.getAlgoNum());

                //add to complementary list
                complementaryList.add(calcQuery(compQuery));
            }
        }

        for (Double complementary : complementaryList) {
            denominatorSum += complementary;

            //the first one is on the zero
            if (denominatorSum != 0) {
                plusCount++;
            }
        }

        denominatorSum += numeratorSum;

        double queryProbSum = numeratorSum / denominatorSum;


        String queryAns = String.format("%.5f",queryProbSum) + "," + plusCount + "," + mulCount;
        file.writeFile(queryAns);

    }


    /**
     * Variable Elimination Algorithm - Elimination order by the alphabet
     * @param query
     */
    public void secondAlgo(myQuery query) {

        EliminationAlgo(query,"alphabet");

    }

    /**
     * Variable Elimination Algorithm - Elimination order by num of rows each variable shown in the factors
     * @param query
     */
    public void thirdAlgo(myQuery query) {

        EliminationAlgo(query,"varRows");
    }



    /**
     *
     * @param query
     * @return query probability
     */
    private double calcQuery(myQuery query) {

        //make all possible permutations for hiddens values
        ArrayList<HashMap<String, Variable>> hidenPermutations = listPermutationsRecursive( getHiddens(query) );

        return calcQueryProbability(hidenPermutations, knownVariebles);
    }



    /**
     * look for all the variables that no query or evidence variables of the Query
     * @param query
     * @return all the hiddens of the cureent query
     */
    private ArrayList<String> getHiddens(myQuery query) {
        knownVariebles = new HashMap<>();
        ArrayList<String> hiddenVariebles = new ArrayList<>();
        Variable parmToMap;


        parmToMap = query.getQueryVariable();

        // insert the query variable
        knownVariebles.put(parmToMap.getVariableName(),parmToMap);

        // insert the evidence variable
        for (int i = 0; i < query.getEvidenceVariables().size(); i++) {
            parmToMap = query.getEvidenceVariables().get(i);
            knownVariebles.put(parmToMap.getVariableName(),parmToMap);
        }

        // insert all the hidden variables to list
        for (Map.Entry<String, Node> varEntry : Vars.entrySet()) {
            if (!knownVariebles.containsKey(varEntry.getKey())) {
                hiddenVariebles.add(varEntry.getValue().getVarName());
            }
        }

        return hiddenVariebles;
    }


    /**
     * calculate the probability for each hidden values permutation
     * @param hidenPermutations
     * @param nothiden
     * @return the probability
     */
    private double calcQueryProbability(ArrayList<HashMap<String, Variable>> hidenPermutations, HashMap<String, Variable> nothiden) {
        double result = 0 ;

        //for every hidden values permutation (num of pluses in the algorithm)
        for (HashMap<String, Variable> hidenMap : hidenPermutations) {
            double currentPermutationSum  , varMultiplying = 1;

            //for every variable
            for (Map.Entry<String, Node> varEntry : Vars.entrySet()) {
                Node currentNode = varEntry.getValue();
                Variable[][] currentCPT = currentNode.getCPT();

                boolean findTheVal = false;

                //iterate on the CPT and check if its the correct row&col answer
                for (int i = 0; i < currentCPT.length && !findTheVal; i++) {
                    boolean correctRow = true;

                    for (int j = 0; j < currentNode.getParents().length && correctRow; j++) {
                        if(!isCorrectCPTcell(currentNode,hidenMap,nothiden,i,j)){
                            correctRow = false;
                        }
                    }

                    if(correctRow){
                        int correctCol ;

                        //the checked var is not hidden
                        if(nothiden.get(currentNode.getVarName()) != null) {
                            correctCol = findValColumn(nothiden.get(currentNode.getVarName()).getValue(), currentNode.getValues());
                        }else{// the checked var is hidden
                            correctCol = findValColumn(hidenMap.get(currentNode.getVarName()).getValue(), currentNode.getValues());
                        }

                        //need to calculate the complementary
                        if(correctCol < 0){
                            double complementary = clacComplementary(currentNode, i);
                            currentPermutationSum = 1-complementary;
                        }else {
                            correctCol += currentNode.getParents().length;
                            currentPermutationSum = Double.parseDouble(currentCPT[i][correctCol].getValue());
                        }

                        //probability multiply
                        if(varMultiplying!=1){
                            mulCount++;
                        }
                        varMultiplying *= currentPermutationSum;
                        findTheVal = true;
                    }
                }
            }

            if(result != 0){
                plusCount++;
            }
            result += varMultiplying;
        }
        return result ;
    }

    /**
     *
     * @param currentNode
     * @param i
     * @return complementary ans
     */
    private double clacComplementary(Node currentNode, int i) {
        double complementary = 0;
        for (int j = currentNode.getParents().length; j < currentNode.getCPT()[0].length; j++) {
            complementary += Double.parseDouble(currentNode.getCPT()[i][j].getValue());
        }
        return  complementary;
    }

    /**
     *
     * @param currentNode
     * @param hidenMap
     * @param nothiden
     * @param i
     * @param j
     * @return true if its correct cell
     */
    private boolean isCorrectCPTcell(Node currentNode, HashMap<String, Variable> hidenMap, HashMap<String, Variable> nothiden, int i, int j) {
        Variable tmp;
        return ((tmp = nothiden.get(currentNode.getParents()[j])) != null &&  tmp.equals(currentNode.getCPT()[i][j]))
                || ((tmp = hidenMap.get(currentNode.getParents()[j])) != null && tmp.equals(currentNode.getCPT()[i][j]));
    }

    /**
     *
     * @param value
     * @param values
     * @return the column of the value
     */
    private int findValColumn(String value, String[] values) {
        for (int i = 0; i < values.length-1; i++) {
            if(values[i].substring(1).equals(value)){
                return i;
            }
        }
        return -1;
    }

    /**
     * make map for every permutation of the hidden values
     * @param list
     * @return ArrayList of all the permutation
     */
    private ArrayList<HashMap<String, Variable>> listPermutationsRecursive(ArrayList<String> list) {

        //stop condition
        if (list.size() == 0) {
            ArrayList<HashMap<String, Variable>> result = new ArrayList<>();
            result.add(new HashMap<>());
            return result;
        }

        ArrayList<HashMap<String, Variable>> resultLists = new ArrayList<HashMap<String, Variable>>();

        //calculate the permutation without the first hidden
        String firstElement = list.remove(0);
        ArrayList<HashMap<String, Variable>> recursiveReturn = listPermutationsRecursive(list);

        //for every calculated map add the first hidden with differnt value
        for (HashMap<String, Variable> map: recursiveReturn) {

            String [] hiddenValues = Vars.get(firstElement).getValues();
            for(String val : hiddenValues){
                HashMap<String, Variable> tmp = new HashMap<>(map);
                tmp.put(firstElement,new Variable(firstElement ,val.substring(1)));
                resultLists.add(tmp);
            }
        }
        return resultLists;
    }

    /**
     * Variable Elimination Algorithm
     * @param query
     * @param eliminationType
     */
    private void EliminationAlgo(myQuery query, String eliminationType) {

        plusCount = 0;
        mulCount = 0;

        // find the hiddens of the current query
        ArrayList<String> hiddenList = getHiddens(query);

        // convert all the CPT's to factors
        ArrayList<Factor> factorsList = initialFactors(query);


        if(eliminationType.equals("alphabet")){

            // sort the hidden by the alphabet (ABC)
            hiddenList.sort(String::compareTo);

        }else if(eliminationType.equals("varRows")){

            //sort the hidden by num of rows in the factors - new object for that - Hidden

            ArrayList<Hidden> sortedHiddenList = new ArrayList<>();
            for(String hiddenName : hiddenList){
                Hidden currentHidden = new Hidden(hiddenName);

                for(Factor factor : factorsList){
                    if(factor.getVariables().containsKey(hiddenName)) {
                        currentHidden.setRowsAppearanseCount(currentHidden.getRowsAppearanseCount() + factor.getFactorTable().size());
                    }
                }
                sortedHiddenList.add(currentHidden);
            }
            sortedHiddenList.sort(Hidden::compareTo);

            hiddenList.clear();
            for(Hidden hidden : sortedHiddenList){
                hiddenList.add(hidden.getName());
            }

        }

        while (!hiddenList.isEmpty()){

            //hidden that we Join and eliminate
            String Hidden = hiddenList.remove(FIRST_ELEMENT);

            // all the factors that contains this hidden
            ArrayList<Factor> hiddenFactors = findFacrotsToJoin(factorsList, Hidden);
            Factor afterJoinFactor = new Factor();


            while(!hiddenFactors.isEmpty()){
                Factor Factor_1 = hiddenFactors.remove(FIRST_ELEMENT);

                //check if that is only one factor in the list
                if(!hiddenFactors.isEmpty()) {
                    Factor Factor_2 = hiddenFactors.remove(FIRST_ELEMENT);

                    //Join between 2 factors
                    afterJoinFactor = JoinFactors(Factor_1, Factor_2, Hidden);

                    //update lists after join -
                    // add to the current hidden the new after join factor and remove from the main factor list the old factors
                    hiddenFactors.add(afterJoinFactor);
                    hiddenFactors.sort(Factor::compareTo);//the smallest factor always first
                    factorsList.remove(Factor_1);
                    factorsList.remove(Factor_2);

                }else{// its only one factor for this hidden - need to eliminate hidden column before proceed
                    //the Elimination
                    Elimination(afterJoinFactor);
                    factorsList.add(afterJoinFactor);
                }
            }

        }

        //final Join between all the factors that left
        while (factorsList.size() > 1){
            Factor resultQuery;
            Factor factor = factorsList.remove(FIRST_ELEMENT);
            if(!factorsList.isEmpty()) {
                resultQuery = JoinFactors(factor, factorsList.remove(FIRST_ELEMENT), "ANSWER_QUERY");
            }else{
                resultQuery = factor;
            }
            factorsList.add(resultQuery);
            factorsList.sort(Factor::compareTo);
        }

        //the final result of the query
        double ans = normlizeAns(factorsList.get(FIRST_ELEMENT) , query.getQueryVariable());


        String queryAns = String.format("%.5f",ans) + "," + plusCount + "," + mulCount;
        file.writeFile(queryAns);


    }

    /**
     * The Join between factors operation
     * @param factor1
     * @param factor2
     * @param hiddenVar
     * @return the after join new factor
     */
    private Factor JoinFactors(Factor factor1, Factor factor2, String hiddenVar) {
        ArrayList<ArrayList<Variable>> ansFactorTable = new ArrayList<>();
        int parentsNum = 0;
        if(Vars.get(hiddenVar) != null){
            parentsNum = Vars.get(hiddenVar).getParents().length;
        }

        Factor result = new Factor(hiddenVar,parentsNum,ansFactorTable);

        //finds all the mutual variables on the 2 factors
        ArrayList<String> shared = sharedVarsInFactors(factor1.getVariables(), factor2.getVariables());

        //iterate the first factor rows
        for (int i = 0; i < factor1.getFactorTable().size(); i++) {
            ArrayList<Variable> rowFactor1 = factor1.getFactorTable().get(i);

            //iterate the second factor rows
            for (int j = 0; j < factor2.getFactorTable().size(); j++) {
                double mulAns;
                boolean canMerge = true;

                ArrayList<Variable> rowFactor2 = factor2.getFactorTable().get(j);

                //for every mutual variable on the factors - find the column and check if equals
                for (String sharedVar : shared) {
                    canMerge = true;

                    if(factor1.getVariables().get(sharedVar) != null && factor2.getVariables().get(sharedVar) != null){
                        //the column of the mutual var in factor 1
                        int column1 = factor1.getVariables().get(sharedVar);
                        //the column of the mutual var in factor 2
                        int column2 = factor2.getVariables().get(sharedVar);

                        // if the mutual variable has the same value
                        if (!rowFactor1.get(column1).equals(rowFactor2.get(column2))) {
                            canMerge = false;
                        }
                    }else{// cant merge the rows
                        canMerge = false;
                    }
                }

                ArrayList<Variable> ansRow = new ArrayList<>();
                //rows merging
                if(canMerge){
                    //the value for the result row
                    String valAns = rowFactor2.get(rowFactor2.size()-1).getVariableName();

                    //the multiplying of the rows probability
                    mulAns = Double.parseDouble(rowFactor1.get(rowFactor1.size()-1).getValue()) *
                            Double.parseDouble(rowFactor2.get(rowFactor2.size()-1).getValue());
                    mulCount++;

                    //the merge
                    ArrayList<Variable> tmpRow = new ArrayList<>(rowFactor1);
                    tmpRow.remove(rowFactor1.size()-1);
                    tmpRow.removeAll(rowFactor2);
                    ansRow.addAll(rowFactor2);
                    ansRow.remove(rowFactor2.size()-1);
                    ansRow.addAll(tmpRow);

                    //the new probability row insert
                    ansRow.add(new Variable(valAns, String.valueOf(mulAns)));

                    //add to the factor table
                    ansFactorTable.add(ansRow);
                }
            }
        }

        //find the column for every variable in the factor
        generateVariables(result);

        return result;
    }

    /**
     *
     * @param factorsList
     * @param hiddenVariable
     * @return all the factors that contains the hidden Variable
     */
    private ArrayList<Factor> findFacrotsToJoin(ArrayList<Factor> factorsList, String hiddenVariable) {
        ArrayList<Factor> factorsToJoin = new ArrayList<>();
        for (Factor factor : factorsList) {
            if (factor.getVariables().containsKey(hiddenVariable)  && !factorsToJoin.contains(factor)) {
                factorsToJoin.add(factor);
            }
        }

        factorsToJoin.sort(Factor::compareTo);//the smallest factor first

        return factorsToJoin;
    }

    /**
     * Eliminate the hidden variable from the factor
     * @param afterJoinFactor
     */
    private void Elimination(Factor afterJoinFactor) {

        ArrayList<ArrayList<Variable>> ansTable = new ArrayList<>();

        while (!afterJoinFactor.getFactorTable().isEmpty()){
            // array of probabilities
            double[] probabilityAns = new double[Vars.get(afterJoinFactor.getVarName()).getValues().length];
            plusCount += (probabilityAns.length-1);
            int count =0;

            //the final row
            ArrayList<Variable> ansRow = afterJoinFactor.getFactorTable().remove(FIRST_ELEMENT);
            String valAns = ansRow.get(ansRow.size()-1).getVariableName();
            probabilityAns[count++] = Double.parseDouble(ansRow.remove(ansRow.size()-1).getValue());

            //removes the probability and the eliminate variable from answer
            for (int j = 0; j < ansRow.size(); j++){
                Variable var = ansRow.get(j);
                if(var.getVariableName().equals(afterJoinFactor.getVarName())){
                    ansRow.remove(var);
                    j--;
                }
            }

            //delete rows and saves their probabilities
            ArrayList<ArrayList<Variable>> rowsToDell = new ArrayList<>();
            for (ArrayList<Variable> row : afterJoinFactor.getFactorTable()){
                if(row.containsAll(ansRow)){
                    probabilityAns[count++] = Double.parseDouble(row.get(row.size()-1).getValue());
                    rowsToDell.add(row);
                }
            }
            afterJoinFactor.getFactorTable().removeAll(rowsToDell);

            //probability calculation
            double plusAns = 0;
            for (int i = 0; i < probabilityAns.length; i++) {
                plusAns += probabilityAns[i];
            }
            String result = String.valueOf(plusAns);
            ansRow.add(new Variable(valAns,result));

            ansTable.add(ansRow);
        }
        afterJoinFactor.setFactorTable(ansTable);
        generateVariables(afterJoinFactor);

    }

    /**
     * the func that call to CPTtpFactor func foe every variable
     * @param query
     * @return
     */
    private ArrayList<Factor> initialFactors(myQuery query) {
        ArrayList<Factor> factorsList = new ArrayList<>();
        HashMap<String,Variable> evidenceMap =  new HashMap<>();

        //Variable queryVariable = query.getQueryVariable();
        ArrayList<Variable> evidenceVariables = query.getEvidenceVariables();
        for(Variable evidence : evidenceVariables ){
            evidenceMap.put(evidence.getVariableName(), evidence);
        }

        // factor for eche variable
        for (Map.Entry<String, Node> varEntry : Vars.entrySet()) {
            // if the variable not evidence or ancestor - no need factor
            if (isAncestor(varEntry.getValue(),query.getQueryVariable(),evidenceMap)) {
                //convert cpt to factor and add to factors list
                factorsList.add(CPTtoFactor(varEntry.getValue(), evidenceMap));
            }
        }

        return factorsList;
    }

    /**
     * convert givens variable CPT (Variable [][]) to factor (ArrayList<ArrayList<Variable>>)
     * @param var
     * @param evidenceMap
     * @return factor
     */
    private Factor CPTtoFactor(Node var, HashMap<String, Variable> evidenceMap) {
        Factor factor = new Factor();

        factor.setVarName(var.getVarName());
        factor.setVarParentsNum(var.getParents().length);


        for (int i = 0; i < var.getCPT().length; i++) {
            //every row in the CPT became to ArrayList in the factor
            ArrayList<Variable> row = new ArrayList<>(Arrays.asList(var.getCPT()[i]));

            //rows with opposite evidance value no nedded in the factor
            boolean neededRow = true;

            //check for every variable if he needed in the row - evidance variables no needed
            for (Map.Entry<String, Variable> evi : evidenceMap.entrySet()) {
                if (neededRow) {
                    Variable evidence = evi.getValue();

                    //remove all the rows that has no like query value evidence variable
                    if (!needToAddRow(evidence, row))
                        neededRow = false;
                    if (row.contains(evidence))
                        row.remove(evidence);// remove all the evidence variables
                }
            }

            // if the variable is evidence , it has the same value as the query
            if(evidenceMap.containsKey(var.getVarName())){
                boolean findProbRow = false;
                double complementary = 0;


                for (int j = var.getParents().length; j < row.size(); j++) {
                    String checkedVar = row.get(j).getVariableName();

                    //if the value in the cpt is the needed value - mark hes probability
                    if (checkedVar.equals(evidenceMap.get(var.getVarName()).getValue())) {
                        findProbRow = true;
                    } else {// if not - calc the complementary
                        complementary += Double.parseDouble(row.get(j).getValue());
                        row.remove(j--);
                    }
                }

                // no find the probability row in the cpt - need to calculate the complementary
                if (!findProbRow) {
                    complementary = 1 - complementary;
                    row.add(new Variable(evidenceMap.get(var.getVarName()).getValue(), String.valueOf(complementary)));
                }

                // add the row to factor table
                if (neededRow)
                    factor.getFactorTable().add(row);

            }else {// the variable not an evidence - needed hes own column in the factor

                //find the index to add the variables column
                int index = var.getParents().length;
                for (int j = 0; j < var.getParents().length; j++) {
                    if(evidenceMap.containsKey(var.getParents()[j])){
                        index --;
                    }
                }

                //the index of the probability of the row
                int probAnsCell = index+1;


                for (int j = 0; j < var.getValues().length; j++) {
                    if (neededRow) {
                        boolean noProbForVar = true;
                        double complementary = 0;

                        ArrayList<Variable> editedRow = new ArrayList<>(row);

                        // removes the evidance variables
                        if (editedRow.get(index).getVariableName().equals(var.getVarName())) {
                            editedRow.remove(index);
                        }
                        editedRow.add(index, new Variable(var.getVarName(), var.getValues()[j].substring(1)));

                        //the probability value doesn't match, need to find or calculate the right value
                        if (!editedRow.get(probAnsCell).getVariableName().equals(editedRow.get(index).getValue())) {
                            ArrayList<String> ansValue = new ArrayList<>(Arrays.asList(var.getValues()));

                            //calculation of the complementary and remove the no needed value
                            complementary += Double.parseDouble(editedRow.get(probAnsCell).getValue());
                            ansValue.remove(" " + editedRow.get(probAnsCell).getVariableName());

                            // look for the procced of the CPT - metby their the right value
                            for (int k = 1; k < var.getValues().length - 1 && noProbForVar; k++) {

                                //finds the needed probability
                                if (editedRow.get(probAnsCell + k).getVariableName().equals(editedRow.get(index).getValue())) {
                                    editedRow.set(probAnsCell, editedRow.get(probAnsCell + k));

                                    for (int l = probAnsCell+1; l < editedRow.size(); l++) {
                                        editedRow.remove(l);
                                    }

                                    //mark that finds the nedded probability
                                    noProbForVar = false;
                                } else {
                                    //add to calculation of the complementary and removes the no needed column
                                    complementary += Double.parseDouble(editedRow.get(probAnsCell + k).getValue());
                                    ansValue.remove(" "+editedRow.get((probAnsCell + k)).getVariableName());
                                }
                            }

                            if (noProbForVar) {//its no ans that we find - calculate the complementary ans
                                complementary = 1 - complementary;
                                editedRow.set(probAnsCell, new Variable(ansValue.get(0), String.valueOf(complementary)));
                                for (int l = probAnsCell+1; l < editedRow.size(); l++) {
                                    editedRow.remove(l);
                                }
                            }

                        }else {// the probability in the table is the right - just remove the next unnecessary columns
                            for (int l = probAnsCell+1; l < editedRow.size(); l++) {
                                editedRow.remove(l);
                            }
                        }

                        //add the new row to the factor
                        factor.getFactorTable().add(editedRow);
                    }
                }
            }
        }

        //find all the variables of the factor and their column - for Join uses
        generateVariables(factor);

        return factor;
    }

    /**
     * check if the variable is ancestor of the query variable or the evidence variables
     * @param var
     * @param queryVariable
     * @param evidence
     * @return true if the variable is ancestor
     */
    private boolean isAncestor(Node var, Variable queryVariable, HashMap<String, Variable> evidence) {
        boolean ans;

        //if the variable is evisence or query its automaticly true
        if (queryVariable.getVariableName().equals(var.getVarName()) || evidence.containsKey(var.getVarName())) {
            return true;
        }

        //check for the query
        ans = isAncestorRecursive(var, Vars.get(queryVariable.getVariableName()).getParents());

        //check for the evidence
        if (!ans) {
            for (Map.Entry<String, Variable> evidenceEntry : evidence.entrySet()) {
                ans = isAncestorRecursive(var, Vars.get(evidenceEntry.getValue().getVariableName()).getParents());
            }
        }

        return ans;
    }

    /**
     * recursive check throw the parents of the parents
     * @param var
     * @param parents
     * @return
     */
    private boolean isAncestorRecursive(Node var , String[] parents){
        if(parents.length == 0){
            return false;
        }

        for (String parent : parents) {
            if (var.getVarName().equals(parent)) {
                return true;
            }else{
                boolean ans = isAncestorRecursive(var,Vars.get(parent).getParents());
                if(ans){
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * finds all the mutual variables on the 2 factors
     * @param vars1
     * @param vars2
     * @return
     */
    private ArrayList<String> sharedVarsInFactors(HashMap<String, Integer> vars1, HashMap<String, Integer> vars2) {

        ArrayList<String> result = new ArrayList<>(vars1.keySet());
        result.retainAll(vars2.keySet());

        return result;
    }

    /**
     * final normalization with Alfa
     * @param factor
     * @param queryVariable
     * @return query answer after normalization
     */
    private double normlizeAns(Factor factor, Variable queryVariable) {
        double queryAns = 0 , Alfa = 0;

        //the first one is plus on zero
        plusCount += (factor.getFactorTable().size()-1);

        for(ArrayList<Variable> row : factor.getFactorTable()){
            if(row.contains(queryVariable)){
                queryAns = Double.parseDouble(row.get(row.size()-1).getValue());
            }

            Alfa += Double.parseDouble(row.get(row.size()-1).getValue());
        }

        return queryAns*(1/Alfa);
    }

    /**
     * find all the variables of the factor and their column - for Join uses
     * @param factor
     */
    private void generateVariables(Factor factor) {
        factor.setVariables(new HashMap<>());

        // generate the variables map of the current factor
        for (ArrayList<Variable> row : factor.getFactorTable()) {
            for (int j = 0; j < row.size()-1; j++) {
                String varName = row.get(j).getVariableName();

                if (!factor.getVariables().containsKey(varName)  && Vars.containsKey(varName)) {
                    // insert the var and the column of the var
                    factor.getVariables().put(varName,j);
                }
            }
        }
    }

    /**
     *
     * @param evidence
     * @param row
     * @return true if the row not contains evidance variable with no like query value
     */
    private boolean needToAddRow(Variable evidence, ArrayList<Variable> row) {
        Variable tmp = new Variable(evidence);

        for (String value : Vars.get(tmp.getVariableName()).getValues()){
            value = value.substring(1);
            if(!tmp.getValue().equals(value)){
                tmp.setValue(value);

                if(row.contains(tmp)){
                    return false;
                }
            }
        }
        return true;
    }


}
