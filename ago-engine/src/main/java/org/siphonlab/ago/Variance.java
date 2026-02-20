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

public enum Variance {
    Invariance(0),
    Covariance(1),
    Contravariance(2);

    final int value;
    Variance(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public byte byteValue() {
        return (byte)value;
    }

    public static Variance of(int value){
        return switch (value){
            case 0 -> Invariance;
            case 1 -> Covariance;
            case 2 -> Contravariance;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
