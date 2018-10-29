package com.tag.management.nfc.engine;

/*
https://github.com/mmk2410/morse-converter-android/blob/master/app/src/main/java/de/marcelkapfer/morseconverter/engine/EncodeNormalMorseManager.java
 */

import com.google.firebase.perf.metrics.AddTrace;

public class EncodeMorseManager {

    @AddTrace(name = "getEncodedString", enabled = true)
    public static String getEncodedString(String inputMessage) {
        if(inputMessage.equals("")){
            return "Please enter at least one character";
        } else {
            // Converts the input string into a StringBuffer
            StringBuffer message = new StringBuffer(inputMessage);
            // Delete the space at the end which is automatically set through some keyboards
            if (message.toString().endsWith(" ")) {
                message = message.deleteCharAt(message.length() - 1);
            }
            // Declaring variables
            String input;
            StringBuffer output = new StringBuffer();
            input = message.toString() + "   ";
            StringBuffer inputToSign = new StringBuffer(input);
            while (!inputToSign.toString().equals("   ")) {
                int d = 0;
                boolean signFull = true;
                StringBuffer sign = new StringBuffer();
                while (signFull) {
                    if (inputToSign.toString().startsWith("       ")) {
                        output.append(" ");
                        inputToSign.delete(d, d + 7);
                    }
                    if (inputToSign.toString().startsWith("\n")) {
                        output.append("\n");
                        inputToSign.deleteCharAt(inputToSign.indexOf("\n"));
                    }
                    if (inputToSign.toString().substring(d, d + 3).equals("   ")) {
                        if (d == 0) {
                            inputToSign.delete(0, 3);
                        } else {
                            sign.replace(0, sign.length(), inputToSign
                                    .toString().substring(0, d));
                            inputToSign.delete(0, d);
                            signFull = false;
                        }
                    } else {
                        d++;
                    }
                }
                if(sign.toString().equals(".-")) {
                    output.append("A");
                } else if (sign.toString().equals("-...")) {
                    output.append("B");
                } else if (sign.toString().equals("-.-.")) {
                    output.append("C");
                } else if (sign.toString().equals("-..")) {
                    output.append("D");
                } else if (sign.toString().equals(".")) {
                    output.append("E");
                } else if (sign.toString().equals("..-.")) {
                    output.append("F");
                } else if (sign.toString().equals("--.")) {
                    output.append("G");
                } else if (sign.toString().equals("....")) {
                    output.append("H");
                } else if (sign.toString().equals("..")) {
                    output.append("I");
                } else if (sign.toString().equals(".---")) {
                    output.append("J");
                } else if (sign.toString().equals("-.-")) {
                    output.append("K");
                } else if (sign.toString().equals(".-..")) {
                    output.append("L");
                } else if (sign.toString().equals("--")) {
                    output.append("M");
                } else if (sign.toString().equals("-.")) {
                    output.append("N");
                } else if (sign.toString().equals("---")) {
                    output.append("O");
                } else if (sign.toString().equals(".--.")) {
                    output.append("P");
                } else if (sign.toString().equals("--.-")) {
                    output.append("Q");
                } else if (sign.toString().equals(".-.")) {
                    output.append("R");
                } else if (sign.toString().equals("...")) {
                    output.append("S");
                } else if (sign.toString().equals("-")) {
                    output.append("T");
                } else if (sign.toString().equals("..-")) {
                    output.append("U");
                } else if (sign.toString().equals("...-")) {
                    output.append("V");
                } else if (sign.toString().equals(".--")) {
                    output.append("W");
                } else if (sign.toString().equals("-..-")) {
                    output.append("X");
                } else if (sign.toString().equals("-.--")) {
                    output.append("Y");
                } else if (sign.toString().equals("--..")) {
                    output.append("Z");
                } else if (sign.toString().equals("-----")) {
                    output.append("0");
                } else if (sign.toString().equals(".----")) {
                    output.append("1");
                } else if (sign.toString().equals("..---")) {
                    output.append("2");
                } else if (sign.toString().equals("...--")) {
                    output.append("3");
                } else if (sign.toString().equals("....-")) {
                    output.append("4");
                } else if (sign.toString().equals(".....")) {
                    output.append("5");
                } else if (sign.toString().equals("-....")) {
                    output.append("6");
                } else if (sign.toString().equals("--...")) {
                    output.append("7");
                } else if (sign.toString().equals("---..")) {
                    output.append("8");
                } else if (sign.toString().equals("----.")) {
                    output.append("9");
                } else if (sign.toString().equals(".-.-")) {
                    output.append("Ä");
                } else if (sign.toString().equals("---.")) {
                    output.append("Ö");
                } else if (sign.toString().equals("..--")) {
                    output.append("Ü");
                } else if (sign.toString().equals("...--...")) {
                    output.append("ß");
                } else if (sign.toString().equals("----")) {
                    output.append("CH");
                } else if (sign.toString().equals(".-.-.-")) {
                    output.append(".");
                } else if (sign.toString().equals("--..--")) {
                    output.append(",");
                } else if (sign.toString().equals("---...")) {
                    output.append(":");
                } else if (sign.toString().equals("-.-.-.")) {
                    output.append(";");
                } else if (sign.toString().equals("..--..")) {
                    output.append("?");
                } else if (sign.toString().equals("-.-.--")) {
                    output.append("!");
                } else if (sign.toString().equals("-....-")) {
                    output.append("-");
                } else if (sign.toString().equals("..--.-")) {
                    output.append("_");
                } else if (sign.toString().equals("-.--.")) {
                    output.append("(");
                } else if (sign.toString().equals("-.--.-")) {
                    output.append(")");
                } else if (sign.toString().equals(".----.")) {
                    output.append("'");
                } else if (sign.toString().equals("-...-")) {
                    output.append("=");
                } else if (sign.toString().equals(".-.-.")) {
                    output.append("RESTAURANT");
                } else if (sign.toString().equals("-..-.")) {
                    output.append("/");
                } else if (sign.toString().equals(".--.-.")) {
                    output.append("@");
                } else if (sign.toString().equals("-.-.-")) {
                    output.append("RETAIL");
                } else if (sign.toString().equals("-...-")) {
                    output.append("LIMITED");
                } else if (sign.toString().equals("...-.")) {
                    output.append("TRADING");
                } else if (sign.toString().equals("...-.-")) {
                    output.append("Ltd");
                } else if (sign.toString().equals("...---...")) {
                    output.append("Pty");
                } else if (sign.toString().equals("........")) {
                    output.append("NL");
                } else {
                    return "Code not listed or wrong.";
                }
            }
            return output.toString();
        }
    }
}