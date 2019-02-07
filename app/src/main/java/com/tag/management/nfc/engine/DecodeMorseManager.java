package com.tag.management.nfc.engine;
/*
https://github.com/mmk2410/morse-converter-android/blob/master/app/src/main/java/de/marcelkapfer/morseconverter/engine/DecodeNormalMorseManager.java
 */

import com.google.firebase.perf.metrics.AddTrace;

public class DecodeMorseManager {

    @AddTrace(name = "getDecodedString", enabled = true)
    public static String getDecodedString(String inputMessage) {
        if (inputMessage.equals("")) {
            return "Please enter at least one character";
        } else {
            StringBuffer message = new StringBuffer(inputMessage);
            if (message.toString().endsWith(" ")) {
                message = message.deleteCharAt(message.length() - 1);
            }
            // Variables
            StringBuffer input = new StringBuffer();
            input = input.replace(0, input.length(), message.toString().toUpperCase());
            StringBuffer output = new StringBuffer();
            if (input.toString().equals("LETTERSPACE")) {
                output.replace(0, output.length(), "   ");
            } else if (input.toString().equals("LTD")) {
                output.replace(0, output.length(), "...-.-");
            } else if (input.toString().equals("NL")) {
                output.replace(0, output.length(), "........");
            } else if (input.toString().equals("RETAIL")) {
                output.replace(0, output.length(), "-.-.-");
            } else if (input.toString().equals("RESTAURANT")) {
                output.replace(0, output.length(), ".-.-.");
            } else if (input.toString().equals("TRADING")) {
                output.replace(0, output.length(), "...-.");
            } else if (input.toString().equals("LIMITED")) {
                output.replace(0, output.length(), ".-...");
            } else if (input.toString().equals("PTY")) {
                output.replace(0, output.length(), "...---...");
            } else if (input.toString().equals("LETTER SPACE")) {
                output.replace(0, output.length(), "   ");
            } else if (input.toString().equals("WORD SPACE")) {
                output.replace(0, output.length(), "       ");
            } else {
                for (int c = input.length(); c > 0; c--) {
                    if (input.toString().startsWith(" ")) {
                        if (output.toString().endsWith("   ")) {
                            output.delete(output.length() - 3, output.length());
                        }
                        output.append("       ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("\n")) {
                        output.append("\n");
                        input.deleteCharAt(input.indexOf("\n"));
                    } else if (input.toString().startsWith("A")) {
                        output.append(".-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("B")) {
                        output.append("-...   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("C")) {
                        output.append("-.-.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("D")) {
                        output.append("-..   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("E")) {
                        output.append(".   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("F")) {
                        output.append("..-.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("G")) {
                        output.append("--.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("H")) {
                        output.append("....   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("I")) {
                        output.append("..   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("J")) {
                        output.append(".---   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("K")) {
                        output.append("-.-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("L")) {
                        output.append(".-..   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("M")) {
                        output.append("--   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("N")) {
                        output.append("-.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("O")) {
                        output.append("---   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("P")) {
                        output.append(".--.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("Q")) {
                        output.append("--.-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("R")) {
                        output.append(".-.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("S")) {
                        output.append("...   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("T")) {
                        output.append("-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("U")) {
                        output.append("..-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("V")) {
                        output.append("...-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("W")) {
                        output.append(".--   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("X")) {
                        output.append("-..-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("Y")) {
                        output.append("-.--   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("Z")) {
                        output.append("--..   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("0")) {
                        output.append("-----   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("1")) {
                        output.append(".----   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("2")) {
                        output.append("..---   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("3")) {
                        output.append("...--   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("4")) {
                        output.append("....-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("5")) {
                        output.append(".....   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("6")) {
                        output.append("-....   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("7")) {
                        output.append("--...   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("8")) {
                        output.append("---..   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("9")) {
                        output.append("----.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("Ä")) {
                        output.append(".-.-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("Ö")) {
                        output.append("---.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("Ü")) {
                        output.append("..--   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("ß")) {
                        output.append("...--...   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith(".")) {
                        output.append(".-.-.-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith(",")) {
                        output.append("--..--   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith(":")) {
                        output.append("---...   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith(";")) {
                        output.append("-.-.-.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("?")) {
                        output.append("..--..   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("!")) {
                        output.append("-.-.--   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("-")) {
                        output.append("-....-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("_")) {
                        output.append("..--.-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("(")) {
                        output.append("-.--.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith(")")) {
                        output.append("-.--.-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("=")) {
                        output.append("-...-   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("+")) {
                        output.append(".-.-.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("/")) {
                        output.append("-..-.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("@")) {
                        output.append(".--.-.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("'")) {
                        output.append(".----.   ");
                        input.delete(0, 1);
                    } else if (input.toString().startsWith("$")) {
                        output.append("...-..-   ");
                        input.delete(0, 1);
                    } else {
                        return "Code not listed or wrong.";
                    }
                }
                if (output.toString().endsWith("   ")) {
                    output.delete(output.length() - 3, output.length());
                }
            }
            return output.toString();
        }
    }
}