package com.predictor.tools;

import java.util.ArrayList;

import com.predictor.genes.BinaryGene;
import com.predictor.genes.EquationGene;

public class EquationHelper {

	private static int findMatchingClose(String equation, int start) {
		//starts at the char after the open
		int result = -1;
		int parensDeep = 0;
		
		for(int i=start;i<equation.length();i++) {
			if(equation.charAt(i) == '(') {
				//increment parensDeep and keep going
				parensDeep += 1;
			}
			if(equation.charAt(i) == ')') { 
				if(parensDeep == 0) {
					//found our close!
					return i;
				} else {
					//not ours; decrement and keep going
					parensDeep -= 1;
				}
			}
		}
		
		return result;
	}
	
	private static String sortOutParentheticals(String equation, ArrayList<String>parentheticals) {
		return sortOutParentheticals(equation, parentheticals, parentheticals.size(), true);
	}
	
	private static String sortOutParentheticals(String equation, ArrayList<String>parentheticals, int parenNum, boolean canRecurse) {
		String temp = "";
		String result = "";
		int closeIdx = -1;
		
		for(int i=0;i<equation.length();i++) {
			
			if(equation.charAt(i) == '(') {
				//find where the parenthetical closes
				closeIdx = findMatchingClose(equation, i+1);
				
				if(closeIdx==-1) {
					//it doesn't close. The parenthetical is the remainder of the equation.
					//make sure we're not at some weird hanging open-paren nonsense...
					if(i<equation.length()-1) {
						result += 'P';
						result += parenNum;
						temp = equation.substring(i+1, equation.length());
						parentheticals.add(temp);
						parenNum++;
					}
					i = equation.length();
				} else {
					//it closes. Yay? (the other option is MUCH simpler :-/)
					
					temp = equation.substring(i+1, closeIdx);
					
					//wait... is this a 0-length parenthetical? Make sure it isn't...
					if(temp.length() > 0) {
						
						result += 'P';
						result += parenNum;
						
						
						parentheticals.add(temp);
						temp = "";
						parenNum++;
					}	
					
					//skip to the close index regardless
					i = closeIdx;
				}
			} else {
				if(equation.charAt(i) != ')') {
					//skip hanging or leading close parens
					result += equation.charAt(i);
				}
			}

		}
		
		/*
		 * Iterate through the parentheticals for sub-parentheticals.
		 *  Yes--this automagically handles the addition of extra parentheticals! :)
		 */
		if(canRecurse) {
			for(int i=0;i<parentheticals.size();i++) {
				temp = parentheticals.get(i);
				parentheticals.set(i, sortOutParentheticals(temp, parentheticals, parenNum, false));
				parenNum = parentheticals.size();
			}
		}
			
		return result;
	}
	
	private static String extractDouble(String eq, int start) {
		String result = "";
		int end = start+1;
		boolean finished = false;
		char temp;
		
		while(end < eq.length() && !finished) {
			temp = eq.charAt(end);
			if(!(Character.isDigit(temp) || temp == '.')) {
				finished = true;
			} else {
				end++;
			}
		}
		
		result = eq.substring(start, end);
		
		return result;
	}
	
	private static String extractInt(String eq, int start) {
		String result = "";
		int end = start+1;
		boolean finished = false;
		char temp;
		
		while(end < eq.length() && !finished) {
			temp = eq.charAt(end);
			if(!Character.isDigit(temp)) {
				finished = true;
			} else {
				end++;
			}
		}
		
		result = eq.substring(start, end);
		
		return result;
	}
	
	/**
	 * Simply run through the equation left-to-right, ignoring order of operations
	 *  outside of the parentheticals. If a variable DNE, uses 1 as the value to
	 *  avoid dividing by 0.
	 * @param eq
	 * @param paren
	 * @param vars
	 * @param vals
	 * @return the result value for the equation
	 */
	private static double doMathSimple(String eq, ArrayList<String> paren, ArrayList<Character> vars, ArrayList<Double> vals) {
		double result = 0;
		ArrayList<Double>components = new ArrayList<Double>();
		char symbol = ' ';
		int idx = -1;
		char temp;
		String tempStr;		
		
		/*
		 * work through all of the equation sub-components
		 */
		for(int i=0;i<eq.length();i++) {
			temp = eq.charAt(i);
			
			//clean up components--we should never have 2 components w/o a symbol
			while(components.size() > 1) {
				components.remove(1);
			}
			
			if(Character.isLowerCase(temp)) {
				//we've found a variable
				idx = vars.indexOf(temp);
				if(idx!=-1) {
					components.add(vals.get(idx));
				} else {
					components.add(0.0);
				}
			} else if(Character.isDigit(temp)) {
				//found a number -- this should just go into components and we should increment past it.
				
				tempStr = extractDouble(eq, i);
				i += tempStr.length() - 1;
				components.add(Double.valueOf(tempStr));
				
			} else {
				//must be a symbol!
				if(symbol==' ') {
					//if we don't have a symbol yet....
					if(temp == 'P') {
						//Handle parenthetical
						//get the paren ID and handle it
						i++;
						tempStr = extractInt(eq, i);
						if(paren.get(Integer.valueOf(tempStr))!=null) {
							components.add(doMathSimple(paren.get(Integer.valueOf(tempStr)), paren, vars, vals));
						}
						i += tempStr.length() - 1;
					} else {
						/*
						 * make sure this isn't some weird leading symbol
						 *  (we already handled leading E & L earlier!)
						 */
						if(components.size()==1) {
							/*
							 * Only keep it if we have 1 component. We only want to do
							 *  simple equations like "1+2"--if we have the '1', we want
							 *  to be able to accept the '+'. 
							 */
							symbol = temp;
						} else if(components.size()<1){
							//skip the thing. Leading symbols should be dropped
						} else if(components.size()>1){
							//weird trailing symbol. Ignore it and nuke the trailing component
							while(components.size() > 1) {
								components.remove(1);
							}
						}
					}
				} else {
					/*
					 * uh oh... need to sort out what we ran into.
					 *  Options are:
					 *  -Parenthetical
					 *  -Ln
					 *  -E (as in e^x)
					 *  -bad equation
					 */
					if(temp == 'L') {
						//ok, not so bad. Handle LN case!
						//simple solution: move forward until we have a digit (paren) or var and take ln of that.
						while(i < eq.length() && !Character.isDigit(temp) && !Character.isLowerCase(temp)) {
							i++;
							if(i<eq.length()) {
								temp = eq.charAt(i);
							}
						}
						if(i >= eq.length()) {
							 components.add(1.0);
						} else if(Character.isDigit(temp)) {
							components.add(Math.log(doMathSimple(paren.get(Integer.valueOf(""+temp)), paren, vars, vals)));
						} else if(Character.isLowerCase(temp)) {
							idx = vars.indexOf(temp);
							if(idx!=-1) {
								components.add(Math.log(vals.get(idx)));
							} else {
								components.add(1.0);
							}
						}
					} else if(temp == 'E') {
						//also not so bad. Handle e^# case!
						//simple solution: move forward until we have a digit (paren) or var raise e to that.
						while(i < eq.length() && !Character.isDigit(temp) && !Character.isLowerCase(temp)) {
							i++;
							if(i<eq.length()) {
								temp = eq.charAt(i);
							}
						}
						if(i >= eq.length()) {
							 components.add(Math.exp(1));
						} else if(Character.isDigit(temp)) {
							components.add(Math.exp(doMathSimple(paren.get(Integer.valueOf(""+temp)), paren, vars, vals)));
						} else if(Character.isLowerCase(temp)) {
							idx = vars.indexOf(temp);
							if(idx!=-1) {
								components.add(Math.exp(vals.get(idx)));
							} else {
								components.add(1.0);
							}
						}
					} else if(temp == 'P') {
						//Handle parenthetical
						//get the paren ID by incrementing once and updating temp
						i++;
						temp = eq.charAt(i);
						if(paren.get(Integer.valueOf(""+temp))!=null) {
							components.add(doMathSimple(paren.get(Integer.valueOf(""+temp)), paren, vars, vals));
						}
					} else {
						//malformed input; skip!
					}
				}
			}
			if(symbol != ' ' && components.size() == 2) {
				//we can do maths! (huzzah!)
				if(symbol=='+') {
					result = components.get(0) + components.get(1);
				} else if(symbol=='*') {
					result = components.get(0) * components.get(1);
				} else if(symbol=='/') {
					if(components.get(1) == 0) {
						//handle divide by 0 by replacing with 0
						result = 0;
					} else {
						result = components.get(0) / components.get(1);
					}
				} else if(symbol=='^') {
					result = Math.pow(components.get(0), components.get(1));
				}
				symbol = ' ';
				components.remove(1);
				components.set(0, result);
			}
		}
		if(components.size() > 0) {
			result = components.get(0);
		} else {
			result = 0;
		}
		
		return result;
	}
	
	private static double doMathHard(String eq, ArrayList<String> paren, ArrayList<Character> vars, ArrayList<Double> vals) {
		double result = 0;
		
		//do order of operations math
		
		return result;
	}
	
	private static ArrayList<String> splitEquation(String eq) {
		ArrayList<String> result = new ArrayList<String>();
		char temp = ' ';
		String tempStr = "";
		
		for(int i=0;i<eq.length();i++) {
			temp = eq.charAt(i);
			if(Character.isLowerCase(temp)) {
				result.add("" + temp);
			} else if(Character.isDigit(temp)) {
				result.add(extractDouble(eq, i));
			} else if(temp == 'P') {
				tempStr = "P" + extractInt(eq, i+1);
				result.add(tempStr);
				i += tempStr.length();
			} else {
				result.add("" + temp);
			}
		}
		
		return result;
	}
	
	/**
	 * Accept an equation and protect the order of operations rules by
	 *  adding parentheses (yes, this is brute force)
	 * @param equation
	 * @return fixed equation
	 */
	private static String protectOrderOfOperations(String equation) {
			
	//	equation = protectEandLn(equation);
	//	equation = protectMandD(equation);
		
		return equation;
	}
	
	/**
	 * Quietly adds a leading "1.0*" to any equation with an E or L in the
	 *  first operation (equation handler wasn't quite working properly
	 *  and this was a faster fix)
	 * @param eq
	 * @return 'fixed' equation, if appropriate
	 */
	private static String fixLeadingEandL(String eq) {
		char temp;
		if(eq.length() > 0) {
			temp = eq.charAt(0);
			if(temp == 'L' || temp == 'E') {
				return "1.0*" + eq;
			} else {
				return eq;
			}
		} else {
			return null;
		}
	}
	
	public static double solveGenes(String equation, ArrayList<Character> variables, ArrayList<BinaryGene> values) throws IndexOutOfBoundsException{
		ArrayList<Double> extractedValues = new ArrayList<Double>();
		
		for(int i=0;i<values.size();i++) {
			extractedValues.add(values.get(i).getDouble());
		}
		
		return solve(equation, variables, extractedValues);
	}
	
	private static String preprocessInterpretation(String eq, ArrayList<String>parentheticals) {
		String result = "";
		String parenVal;
		
		for(int i=0;i<eq.length();i++) {
			if(eq.charAt(i) != 'P') {
				result+=eq.charAt(i);
			} else {
				//found a parenthetical. Reset variable & increment index.
				parenVal = "";
				i++;
				
				//add open paren to result
				result += "(";
				
				while(i < eq.length() && Character.isDigit(eq.charAt(i))) {
					//build lookup value
					parenVal += eq.charAt(i);
					i++;
				}
				
				//recurse
				result += preprocessInterpretation(parentheticals.get(Integer.valueOf(parenVal)), parentheticals);
				
				//add close paren
				result += ")";
				
				//decrement index (we went one too far in the while loop)
				i--;
			}
		}
		
		return result;
	}
	
	private static double solveLinear(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * x + b;
		
		return result;
	}
	
	private static double solveQuadratic(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double c = values.get(variables.indexOf('c'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * Math.pow(x, 2) + b * x + c;
		
		return result;
	}
	
	private static double solveHex(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double c = values.get(variables.indexOf('c'));
		double d = values.get(variables.indexOf('d'));
		double e = values.get(variables.indexOf('e'));
		double f = values.get(variables.indexOf('f'));
		double g = values.get(variables.indexOf('g'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * Math.pow(x, 6) + b * Math.pow(x, 5) + c * Math.pow(x, 4);
		result += d* Math.pow(x, 3) + e * Math.pow(x, 2) + f * x + g;
		
		return result;
	}
	
	private static double solvePower(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * Math.pow(x, b);
		
		return result;
	}
	
	private static double solveModPower(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double c = values.get(variables.indexOf('c'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * Math.pow(x, b) + c;
		
		return result;
	}
	
	private static double solveExponential(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * Math.exp(b * x);
		
		return result;
	}
	
	private static double solveModExponential(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double c = values.get(variables.indexOf('c'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * Math.exp(b * x) + c;
		
		return result;
	}
	
	private static double solveLog(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * Math.log(x) + b;
		
		return result;
	}
	
	private static double solveModLog(ArrayList<Character> variables, ArrayList<Double> values) {
		double result = 0;
		double a = values.get(variables.indexOf('a'));
		double b = values.get(variables.indexOf('b'));
		double c = values.get(variables.indexOf('c'));
		double x = values.get(variables.indexOf('x'));
		
		result = a * Math.log(b * x) + c;
		
		return result;
	}
	
	public static double solve(EquationGene equation, ArrayList<Character> variables, ArrayList<Double> values) throws IndexOutOfBoundsException{
		double result = 0;
		String simpleEq = "";
		ArrayList<String>parentheticals;	//we can handle N of these

		//Do we have the proper number of variables & values?
		if(variables.size() != values.size()) {
			System.out.println("\n\nVariables: " + variables);
			System.out.println("Values: " + values);
			throw new IndexOutOfBoundsException();
		}
		
		if(equation.getUsableEquation() == null && equation.getParentheticals() == null) {
			
			parentheticals = new ArrayList<String>();
	
			simpleEq = sortOutParentheticals(equation.toString(), parentheticals);
	
			simpleEq = fixLeadingEandL(simpleEq);
			
			for(int i=0;i<parentheticals.size();i++) {
				parentheticals.set(i, fixLeadingEandL(parentheticals.get(i)));
			}
			
			if(equation.getInterpretation().equals("")) {
				equation.setInterpretation(preprocessInterpretation(simpleEq, parentheticals));
			}
			
//			System.out.println(Thread.currentThread().getName() + " storing: " + simpleEq + "\nParens: " + parentheticals);

			equation.setUsableEquation(simpleEq);
			equation.setParentheticals(parentheticals);
		}
		
		/*
		 * Try to cheat and check against the usual 8 formulas
		 *  (basically, why actually try to process them if we know what they are?)
		 */
		if(equation.toString().equals("a*x+b")) {
			return solveLinear(variables, values);
		} else if(equation.toString().equals("a*x*x+b*x+c")) {
			return solveQuadratic(variables, values);
		} else if(equation.toString().equals("a*x*x*x*x*x*x+b*x*x*x*x*x+c*x*x*x*x+d*x*x*x+e*x*x+f*x+g")) {
			return solveHex(variables, values);
		} else if(equation.toString().equals("a*(x^b)")) {
			return solvePower(variables, values);
		} else if(equation.toString().equals("a*(x^b)+c")) {
			return solveModPower(variables, values);
		} else if(equation.toString().equals("a*(E(b*x))")) {
			return solveExponential(variables, values);
		} else if(equation.toString().equals("a*(E(b*x))+c")) {
			return solveModExponential(variables, values);
		} else if(equation.toString().equals("a*Lx+b")) {
			return solveLog(variables, values);
		} else if(equation.toString().equals("a*L(b*x)+c")) {
			return solveModLog(variables, values);
		}
					
		result = doMathSimple(equation.getUsableEquation(), equation.getParentheticals(), variables, values);
		//result = doMathHard(simpleEq, parentheticals, variables, values);
		
		return result;
	}
	
	public static double solve(String equation, ArrayList<Character> variables, ArrayList<Double> values) throws IndexOutOfBoundsException{
		double result = 0;
		String simpleEq = "";
		ArrayList<String>parentheticals;	//we can handle N of these
		
		parentheticals = new ArrayList<String>();

		
		//Do we have the proper number of variables & values?
		if(variables.size() != values.size()) {
			System.out.println("\n\nVariables: " + variables);
			System.out.println("Values: " + values);
			throw new IndexOutOfBoundsException();
		}
		
		simpleEq = sortOutParentheticals(equation, parentheticals);

		simpleEq = fixLeadingEandL(simpleEq);
		
		for(int i=0;i<parentheticals.size();i++) {
			parentheticals.set(i, fixLeadingEandL(parentheticals.get(i)));
		}
		
		/*		
		System.out.println(simpleEq);
		for(int i=0;i<parentheticals.size();i++) {
			System.out.println("" + i + ") " + parentheticals.get(i));
		}*/		
		
		result = doMathSimple(simpleEq, parentheticals, variables, values);
		//result = doMathHard(simpleEq, parentheticals, variables, values);
		return result;
	}
	
	public static void main(String[] args) {
		//Testing main
		
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<Character> variables = new ArrayList<Character>();
		
		values.add(1.0);
		values.add(2.0);
		values.add(3.0);
		variables.add('a');
		variables.add('b');
		variables.add('c');
		
		System.out.println(EquationHelper.solve("Lb", variables, values));

	}

}
