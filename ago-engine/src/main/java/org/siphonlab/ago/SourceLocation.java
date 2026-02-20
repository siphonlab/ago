/*
 * Copyright © 2026 Inshua (inshua@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.siphonlab.ago;


import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class SourceLocation implements Comparable<SourceLocation>{

    private final int end;
    private final int start;

    private String filename;

    private int line;

    private int column;

    private int length;

    public SourceLocation(String filename, int line, int column, int length, int start, int end){
        this.filename = filename;
        this.line = line;
        this.column = column;
        this.length = length;
        this.start = start;
        this.end = end;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return String.format("[%s:%s %s-%s]", this.filename, this.line, this.column, this.length);
    }

    public SourceLocation extend(SourceLocation another){
        assert Objects.equals(another.filename, this.filename);
        var minStart = this.start < another.start? this : another;
        var maxEnd = this.end > another.end ? this : another;
        return new SourceLocation(filename, minStart.line, minStart.column,
                maxEnd.end - minStart.start, minStart.start, maxEnd.end);
    }

    public boolean contains(SourceLocation another){
        assert Objects.equals(this.filename, another.filename);
        return another.start >= this.start && another.end <= this.end;
    }

    @Override
    public int compareTo(SourceLocation another) {
        var r = StringUtils.compare(this.filename, another.filename);
        if(r != 0) return r;
        r = Integer.compare(this.line, another.line);
        if(r !=0) return r;
        r = Integer.compare(this.column,another.column);
        if(r != 0) return r;
        return Integer.compare(this.length, another.length);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
