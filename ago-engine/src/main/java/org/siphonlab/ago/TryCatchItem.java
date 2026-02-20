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

public class TryCatchItem {
    public final  int begin;
    public final int end;
    public final  int handler;
    public final AgoClass[] exceptionClasses;

    public TryCatchItem(int begin, int end, int handler, AgoClass[] exceptionClasses) {
        this.begin = begin;
        this.end = end;
        this.handler = handler;
        this.exceptionClasses = exceptionClasses;
    }

    public int resolve(int pc, AgoClass exceptionClass){
        if(pc >= begin && pc <= handler){
            for (AgoClass agoClass : exceptionClasses) {
                if(agoClass == exceptionClass) return handler;
                if(exceptionClass.isThatOrDerivedFrom(agoClass)){
                    return handler;
                }
            }
        }
        return -1;
    }
}
