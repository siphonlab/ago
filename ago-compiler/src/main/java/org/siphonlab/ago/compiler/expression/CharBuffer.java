package org.siphonlab.ago.compiler.expression;

import java.util.ArrayList;
import java.util.List;

public class CharBuffer {
    private List<String> list = new ArrayList<>();

    private int indexInList = 0;
    private int index = 0;
    private int size = 0;

    public void append(String s){
        list.add(s);
        size += s.length();
    }

    public char peek(){
        return la(0);
    }

    // get next char, if index > length(currString), advance to next string
    public char get(){
        if (indexInList >= list.size()) {
            return  '\0';
        }

        String currentString = list.get(indexInList);
        char ch = currentString.charAt(index);

        // Move to next character
        index++;

        // If we've reached the end of current string, move to next string
        if (index >= currentString.length()) {
            index = 0;
            indexInList++;
        }

        return ch;
    }

    // get char at forward n
    public char la(int n){
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }

        int currentIndexInList = indexInList;
        int currentIdx = index;

        // Move forward n characters
        for (int i = 0; i < n; i++) {
            if (currentIndexInList >= list.size()) {
                return '\0';
            }

            String currentString = list.get(currentIndexInList);
            currentIdx++;

            // If we've reached the end of current string, move to next string
            if (currentIdx >= currentString.length()) {
                currentIdx = 0;
                currentIndexInList++;
            }
        }

        if (currentIndexInList >= list.size()) {
            return '\0';
        }

        return list.get(currentIndexInList).charAt(currentIdx);
    }

    public CharBuffer skip(int n){
        for (int i = 0; i < n; i++) {
            get();
        }
        return this;
    }

    // if peek char(s) is newline, skip the new line char(s)
    public boolean skipNewLineIfPeekIsNewLine(){
        char ch = peek();
        if (ch == '\n') {
            get();
            return true;
        } else if (ch == '\r') {
            get();
            if (peek() == '\n') {
                get(); // Skip the \n after \r
            }
            return true;
        }
        return false;
    }

    // skip whitespaces(\t treat as ' '), maximum skip count is indentSize
    public void skipIndent(int indentSize){
        int skipped = 0;
        while (skipped < indentSize) {
            char ch = peek();
            if (ch == ' ' || ch == '\t') {
                get();
                skipped++;
            } else {
                break;
            }
        }
    }

    // skip whitespaces(\t treat as ' '), till none ws, and calculate the char count
    public int skipWs(){
        int count = 0;
        while (true) {
            char ch = peek();
            if (ch == ' ' || ch == '\t') {
                get();
                count++;
            } else {
                break;
            }
        }
        return count;
    }
}

