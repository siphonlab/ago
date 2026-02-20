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
package org.siphonlab.ago.study;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.siphonlab.ago.Slots;

public class ConcatSlots {

    @Test
    public void test(){
        var slots2 = new Slots(){
            int i1;
            int i2;
            int i3;
            Slots next;
            @Override
            public int getInt(int slot) {
                switch (slot) {
                    case 0:
                        return i1;
                    case 1:
                        return i2;
                    case 2:
                        return i3;
                    default:
                        if (next != null) {
                            return next.getInt(slot - 3);
                        } else {
                            throw new IllegalArgumentException();
                        }
                }
            }

            @Override
            public void setInt(int slot, int value) {
                switch (slot){
                    case 0: i1 = value; break;
                    case 1: i2 = value; break;
                    case 2: i3 = value; break;
                    default:
                        if(next != null){
                            next.setInt(slot - 3, value);
                        } else {
                            throw new IllegalArgumentException();
                        }
                }
            }
        };
        var slots1 = new Slots(){
            int i1;
            int i2;
            int i3;
            Slots next;
            @Override
            public int getInt(int slot) {
                switch (slot) {
                    case 0:
                        return i1;
                    case 1:
                        return i2;
                    case 2:
                        return i3;
                    default:
                        if (next != null) {
                            return next.getInt(slot - 3);
                        } else {
                            throw new IllegalArgumentException();
                        }
                }
            }
            @Override
            public void setInt(int slot, int value) {
                switch (slot){
                    case 0: i1 = value; break;
                    case 1: i2 = value; break;
                    case 2: i3 = value; break;
                    default:
                        if(next != null){
                            next.setInt(slot - 3, value);
                        } else {
                            throw new IllegalArgumentException();
                        }
                }
            }
        };
        slots1.next = slots2;

        slots1.setInt(3, 2);
        slots1.setInt(4, 1);
        slots1.setInt(5, 9);

        Assertions.assertEquals(slots2.getInt(0), 2);
        Assertions.assertEquals(slots2.getInt(1), 1);
        Assertions.assertEquals(slots2.getInt(2), 9);
    }
}
