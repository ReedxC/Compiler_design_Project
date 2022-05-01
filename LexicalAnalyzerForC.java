import java.util.*;
import java.util.regex.*;
import java.io.*;
@SuppressWarnings("unchecked")
public class LexicalAnalyzerForC {
    public static void main(String[] args) {
        
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter the filename of the .C file you want to analyse\nThe filename should be a single word without spaces");
        String filename = sc.next();
        LexicalAnalyzerForC runner = new LexicalAnalyzerForC();
        runner.fileInput(filename);
        System.out.println();
    }

    StringBuilder inputChars = new StringBuilder("");
    LinkedList<String> lexemesList = new LinkedList<>();
    LinkedList<String> commentsList = new LinkedList<>();

    //tokens
    LinkedList<String> operatorsList = new LinkedList<>();//
    LinkedList<String> identifiersList = new LinkedList<>();//
    LinkedList<String> keywordsList = new LinkedList<>();//
    LinkedList<String> separatorsList = new LinkedList<>();//
    LinkedList<String> constantsList = new LinkedList<>();//
    LinkedList<String> stringLiteralsList = new LinkedList<>();//
    LinkedList<String> characterLiteralsList = new LinkedList<>();//
    LinkedList<String> functionsList = new LinkedList<>();//
    LinkedList<String> misspelledWordsList = new LinkedList<>();//
    LinkedList<String> specialCharactersList = new LinkedList<>();//
    LinkedList<String> punctuatorsList = new LinkedList<>();//
    
    LinkedList<Symbols> symbolsList = new LinkedList<>();

    //hashBucket for spellcheck
    LinkedList<String> hashBucket[] = new LinkedList[10];
    
    //pre-defined
    String specialChar = "+-*/={}[]()!@#$%^&*?\\;:\'~`.,><| \"\n\r\t";
    
    String syntaxList[] =  {"+","-","*","/","%",   
                            ",",";",".","->",  
                            "=","+=","/=","*=","-=","%=",  
                            "==","!=","<","<=",">",">=",  
                            "&","&&","|","||","!",  
                            "#","?",":","`","~","@","$","^","\\",  
                            ">>",">>>","<<","<<<",  
                            "++","--",
                            "(",")","{","}","[","]"};
                            
    String arithmetic_symbols[] = {"+","-","*","/","%"};                    //operator
    String separators_symbols[] = {"(",")","{","}","[","]"};                //separator
    String punctuator_symbols[] = {";",",",".","->"};                       //punctuator
    String assignment_symbols[] = {"=","+=","/=","*=","-=","%="};           //operator
    String comparator_symbols[] = {"==","!=","<","<=",">",">="};            //operator
    String logicalOps_symbols[] = {"&","&&","|","||","!"};                  //operator
    String specCharac_symbols[] = {"#","?",":","`","~","@","$","^","\\"};   //specialCharacter
    String binShiftOp_symbols[] = {">>",">>>","<<","<<<"};                  //operator
    String incrementDecrement_symbols[] = {"++","--"};                      //operator
    
    String constantsRegex = "";
    
    String keywords[] = {"auto","break","case","char","const","continue","default","do","double","else","enum","extern","float","for","goto","if","int","long","register",
                                "return","short","signed","sizeof","static","struct","switch","typedef","union","unsigned","void","volatile","while"};                         
    String builtInFunctions[] = {"scanf","printf","isalpha","isdigit","isalnum","islower","isspace","tolower","toupper","isupper","isxdigit","fprintf","fscanf","strcat","strcmp","strcpy",
                                 "strncat","strncmp","strncpy","strlen","strrchr","strchr","strupr","strlwr","strdup","strstr","strrstr","strset","strnset","strtok","abs","floor","round",
                                 "ceil","memset","memcpy","memmove","memcmp","memchr","memicmp","malloc","calloc","realloc","free","include","define","undef","main","ifndef","endif"};
    
    public String listToString(LinkedList<String> list)
    {
        String value = "";
        for(String i:list)
            value=value+i+"\n";
        return value;
    }
    
    boolean isOperator(String s)
    {
        String[] all_operators = {"+","-","*","/","%","=","+=","/=","*=","-=","%=","==","!=","<","<=",">",">=","&","&&","|","||","!",">>",">>>","<<","<<<","++","--"};
        for(int i=0;i<all_operators.length;i++)
            if(all_operators[i].equals(s))
                return true;
        return false;
    }

    boolean isSeparator(String s)
    {
        for(int i=0;i<separators_symbols.length;i++)
            if(separators_symbols[i].equals(s))
                return true;
        return false;
    }

    boolean isConstant(String s)
    {
        if(s.length()==0) return false;
        if(s.length()==1) return Character.isDigit(s.charAt(0));
        if(s.length()==2) return Character.isDigit(s.charAt(0)) && Character.isDigit(s.charAt(1));
        int dots=0;
        for(int i=1;i<s.length()-1;i++)
        {
            char ch = s.charAt(i);
            if(!Character.isDigit(ch))
            {
                if(ch == '.')
                    dots++;
                else
                    return false;
            }
        }
        if(Character.isDigit(s.charAt(0)) && Character.isDigit(s.charAt(s.length()-1))) return true;
        if(dots==1) return true;
        return false;
    }

    boolean isSpecialCharacter(String s)
    {
        for(int i=0;i<specCharac_symbols.length;i++)
            if(specCharac_symbols[i].equals(s))
                return true;
        return false;
    }

    boolean isPunctuator(String s)
    {
        for(int i=0;i<punctuator_symbols.length;i++)
            if(punctuator_symbols[i].equals(s))
                return true;
        return false;
    }

    void createHashTable()
    {
        for(int i=0;i<10;i++)
            hashBucket[i] = new LinkedList<String>();
        
        for(String str:keywords)
        {
            int hashIndex = hashFunction(str);
            hashBucket[hashIndex].add(str);
        }
        for(String str:builtInFunctions)
        {
            int hashIndex = hashFunction(str);
            hashBucket[hashIndex].add(str);
        }       
    }
    
    boolean checkSpelling(String s)
    {
        int index=hashFunction(s);
        for(String str:hashBucket[index])
            if(str.equals(s))
                return true;
            else if(misspelledWord(s,str))
                misspelledWordsList.add(s);
        return false;
    }
    boolean misspelledWord(String incorrect,String correct)
    {
        char[] incorrectWord = incorrect.toCharArray();
        char[] correctWord = correct.toCharArray();
        
        Arrays.sort(incorrectWord);
        Arrays.sort(correctWord);
        if(incorrectWord.length!=correctWord.length)return false;
        for(int i=0;i<incorrectWord.length;i++)
            if(incorrectWord[i] != correctWord[i])
                return false;           
        return true;
    }
    int hashFunction(String s)
    {
        int sum=0;
        int length=s.length();
        
        for(int i=0;i<length;i++)
        {
            int c=(int)s.charAt(i);
            sum+=c;
        }
        return sum%length;
    }
    
    boolean isKeyword(String s)
    {
        for(String str:keywords)
            if(str.equals(s))
                return true;
        return false;
    }
    
    boolean isValidIdentifier(String s)
    {
        return (s.length()>0)?(s.charAt(0)=='_' || Character.isLetter(s.charAt(0))):false;
    }
    
    boolean isInBuiltFunction(String s)
    {
        for(String str:builtInFunctions)
            if(str.equals(s))
                return true;
        return false;
    }
    
    void segregateLexemesIntoTokens() // just segregate the tokens and classify them ... no symboltable is made
    {
        createHashTable();
        for(String str:lexemesList)
        {
            if(isValidIdentifier(str))
            {
                if(isKeyword(str))
                {
                    keywordsList.add(str);
                }
                else if(isInBuiltFunction(str))
                {
                    functionsList.add(str);
                }
                else if(!checkSpelling(str))
                {
                    identifiersList.add(str);
                }    
            }
            else
            {
                if(isOperator(str))
                    operatorsList.add(str);
                else if(isConstant(str))
                    constantsList.add(str);
                else if(isPunctuator(str))
                    punctuatorsList.add(str);
                else if(isSeparator(str))
                    separatorsList.add(str);
                else if(isSpecialCharacter(str))
                    specialCharactersList.add(str);
                else if(isStringLiteral(str))
                    stringLiteralsList.add(str);
                else if(isCharacterLiteral(str))
                    characterLiteralsList.add(str);
                else
                    System.err.println("Error: invalid symbol"+str);
            }
        }
    }

    boolean isStringLiteral(String s)
    {
        return (s.length()<2)?false : (s.charAt(0)=='\"' && s.charAt(s.length()-1)=='\"');
    }
    boolean isCharacterLiteral(String s)
    {
        return (s.length()<3)?false : (s.charAt(0)=='\'' && s.charAt(s.length()-1)=='\'');
    }
    
    void scopeOut()
    {

    }
    
    void extractComments()
    {
        if(inputChars.toString().equals("") || inputChars.length()<3)
            return;
        
        char lastChar = (char)0;
        //int startIndex=-1;
        //int endIndex=-1; 
        for(int i=0;i<inputChars.length();i++)
        {
            String comment = "";
            char c = inputChars.charAt(i);

            if(c=='/' && lastChar == '/')
            {
                comment="";
                for(int j=i-1;true;j++)
                {
                    
                    if(inputChars.charAt(j) == '\n')
                    {   
                        inputChars = inputChars.replace(i-1,j," ");
                        //i=i-1;
                        commentsList.add(comment);
                        break;
                    }
                    comment+=inputChars.charAt(j);
                    if(j==inputChars.length()-1)
                    {
                        System.out.println("Error: comment is not ended");
                        return;
                    }
                }
            }
            else if(c=='*' && lastChar=='/') 
            {
                char last_sub_c=(char)0;
                comment="";
                for(int j=i-1;true;j++)
                {
                    char sub_c=inputChars.charAt(j);
                    comment+=sub_c;
                    if(sub_c == '/' && last_sub_c=='*')
                    {
                        inputChars = inputChars.replace(i-1,j+1," ");
                        //i=j+1;
                        commentsList.add(comment);
                        break;
                    }
                    last_sub_c=sub_c;
                    if(j==inputChars.length()-1)
                    {
                        System.out.println("Error: comment is not ended");
                        return;
                    }
                }
            }
            lastChar=c;  
        }
    }

    boolean isSpecialChar(char c)
    {
        for(int i=0;i<specialChar.length();i++)
            if(c==specialChar.charAt(i))
                return true;
        return false;
    }
    /*
    void getLexemes(String s)
    {
        s=s.trim();
        s=s.replaceAll("\\s+"," ");
        String word="";
        boolean doubleQuoted = false;
        boolean singleQuoted = false;
        for(int i=0;i<s.length();i++)
        {
            char c=s.charAt(i);
            if(isSpecialChar(c))
            {
                //checking quoted string ache ki na
                if(c=='\"' &&!doubleQuoted)
                    doubleQuoted=true;
                else if(c=='\"' &&doubleQuoted)
                {    
                    doubleQuoted=false;
                    lexemesList.add(word);
                    word="";
                }
                else if(c=='\'' &&!singleQuoted)
                    singleQuoted=true;
                else if(c=='\'' &&singleQuoted)
                {    
                    singleQuoted=false;
                    lexemesList.add(word);
                    word="";
                }
                
                if(doubleQuoted)
                {    
                    word+=c;
                    continue;
                }
                else if(singleQuoted)
                {
                    word+=c;
                    continue;
                }
                else if(!word.equals(""))
                {
                    lexemesList.add(word);
                    word="";
                }
                
                
                if(c!=' ')//individual special chars dhukiye ney
                {
                    lexemesList.add(Character.toString(c));
                }
                else
                    c=(char)0;
            }
            else
            {
                word+=c; 
            }  
        }
        
    }
    */
    
    void getLexemes(String s)
    {
        s=s.trim();
        s=s.replaceAll("\\s+"," ");
        StringTokenizer st = new StringTokenizer(s,specialChar,true);
        LinkedList<String> tempList1 = new LinkedList<>();//all separated
        LinkedList<String> tempList2 = new LinkedList<>();//single quotes unite
        LinkedList<String> tempList3 = new LinkedList<>();//double quotes unite
        LinkedList<String> tempList4 = new LinkedList<>();//3 digit operators unite
        LinkedList<String> tempList5 = new LinkedList<>();//2 digit operators unite
        //spaces removed and sent to lexemesList
        while(st.hasMoreTokens())
            tempList1.add(st.nextToken());
        
        boolean doubleQuoted = false;
        boolean singleQuoted = false;
        
        for(int i=0;i<tempList1.size();i++) //tempList1 er single quoted gulo ke eksathe kore tempList2 te store kora
        {
            String tempString = tempList1.get(i);
            if(tempString.equals("'"))
            {
                String word=tempString;
                for(int j=i+1;true;j++)
                {
                    if(j>=tempList1.size())
                    {
                        System.err.println("Error: Unmatched '");
                        return;
                    }
                    word+=tempList1.get(j);
                    if(tempList1.get(j).equals("'"))
                    {   
                        i=j;
                        break;
                    }
                }
                tempList2.add(word);
            }
            else
                tempList2.add(tempString);
        }
        for(int i=0;i<tempList2.size();i++) //tempList2 er double quoted gulo ke eksathe kore tempList3 te store kora
        {
            String tempString = tempList2.get(i);
            if(tempString.equals("\""))
            {
                String word=tempString;
                for(int j=i+1;true;j++)
                {
                    if(j>=tempList2.size())
                    {
                        System.err.println("Error: Unmatched \"");
                        return;
                    }
                    word+=tempList2.get(j);
                    if(tempList2.get(j).equals("\""))
                    {
                        i=j;
                        break;
                    }
                }
                tempList3.add(word);
            }
            else
                tempList3.add(tempString);
        }
         
        
        for(int i=0;i<tempList3.size()-2;i++)//FIX THIS
        {
            String tempString1 = tempList3.get(i);
            String tempString2 = tempList3.get(i+1);
            String tempString3 = tempList3.get(i+2);
            if((tempString1.equals(">") && tempString2.equals(">") && tempString3.equals(">"))||(tempString1.equals("<") && tempString2.equals("<") && tempString3.equals("<")))
            {    
                tempList4.add(tempString1+tempString2+tempString3);
                i+=2;
            }
            else if(i==tempList3.size()-3)
            {
                tempList4.add(tempString1);
                tempList4.add(tempString2);
                tempList4.add(tempString3);
                //  i+=2;
            }
            else
                tempList4.add(tempString1);
        }
        
        for(int i=0;i<tempList4.size()-1;i++)//FIX THIS
        {
            String tempString1 = tempList4.get(i);
            String tempString2 = tempList4.get(i+1);
            if( (tempString1.equals(">") && tempString2.equals(">"))||
                (tempString1.equals("<") && tempString2.equals("<"))||
                
                (tempString1.equals("=") && tempString2.equals("="))||
                (tempString1.equals("<") && tempString2.equals("="))||
                (tempString1.equals(">") && tempString2.equals("="))||
                (tempString1.equals("!") && tempString2.equals("="))||
                
                (tempString1.equals("-") && tempString2.equals(">"))||
                
                (tempString1.equals("&") && tempString2.equals("&"))||
                (tempString1.equals("|") && tempString2.equals("|"))||
                
                (tempString1.equals("+") && tempString2.equals("="))||
                (tempString1.equals("-") && tempString2.equals("="))||
                (tempString1.equals("/") && tempString2.equals("="))||
                (tempString1.equals("*") && tempString2.equals("="))||
                (tempString1.equals("%") && tempString2.equals("="))||
                
                (tempString1.equals("-") && tempString2.equals("-"))||
                (tempString1.equals("+") && tempString2.equals("+"))) 
            {    
                tempList5.add(tempString1+tempString2);
                i+=1;
            }
            else if(i==tempList4.size()-2)
            {
                tempList5.add(tempString1);
                tempList5.add(tempString2);
            }
            else
                tempList5.add(tempString1); 
        }
        
        for(int i=0;i<tempList5.size();i++)
        {
            String tempString = tempList5.get(i);
            if(!tempString.equals(" "))
                lexemesList.add(tempString);    
        }  
    }
   
    void writeOnFile(String filename, String textString)
    {
        try 
        {
            PrintWriter writer = new PrintWriter(checkFile(filename));
            writer.println(textString);
            System.out.println("Printwriter has written on the file "+filename);
            writer.flush();
            writer.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    File checkFile(String filename)
    {
        File file = null;
        try 
        {
            file = new File(filename);
            if(file.createNewFile())
                System.out.println("File has been created.");    
            else
                System.out.println("File exists");
        } 
        catch (Exception e) 
        {
            e.printStackTrace();    
        }
        
        
        return file;
    }

    void fileInput(String filename)
    {
        try
        {
            inputChars=new StringBuilder("");
            File inputFile = new File(filename);
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            Scanner sc = new Scanner(inputFile);
            int c=0;
            while((c=reader.read()) != -1)
            {
                inputChars.append((char)c);  
            }
            inputChars = new StringBuilder(inputChars.toString().trim());
            extractComments();
            getLexemes(inputChars.toString());
            segregateLexemesIntoTokens();
            //inputChars = new StringBuilder(inputChars.toString().replaceAll("(\r\n|\n)","`"));
            //inputChars = new StringBuilder(inputChars.toString().replaceAll("\\p{Space}{2,}"," "));
            //inputChars = new StringBuilder(inputChars.toString().replace("`","\r\n"));
            reader.close();
            sc.close();
            System.out.println();
            System.out.println("Comments:\n"+commentsList);
            System.out.println("Lexemes:\n"+listToString(lexemesList));
            
            System.out.println("Identifiers:\n"+identifiersList);
            System.out.println("Keywords:\n"+keywordsList);
            System.out.println("Functions:\n"+functionsList);
            System.out.println("Misspelled Words:\n"+misspelledWordsList);
            
            System.out.println("Operators:\n"+operatorsList);
            System.out.println("Punctuators:\n"+punctuatorsList);
            System.out.println("Separators:\n"+separatorsList);
            System.out.println("Constants:\n"+constantsList);
            System.out.println("Special Characters:\n"+specialCharactersList);
            
            System.out.println("String literals:\n"+stringLiteralsList);
            System.out.println("Character literals:\n"+characterLiteralsList);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}

class Symbols
{
    String lexeme;
    String tokenClass;
    String var_type;
    String var_name;
    String data;
    String scope;
    Symbols(String t,String n, String d,String l,String tc)
    {
        var_type=t;
        var_name=n;
        data=d;
        lexeme=l;
        tokenClass=tc;
    }
    Symbols()
    {
        var_type="";
        var_name="";
        data="";
        lexeme="";
        tokenClass="";
        scope="";
    }
}