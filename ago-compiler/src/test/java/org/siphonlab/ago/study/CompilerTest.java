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

import org.agrona.collections.IntArrayList;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.Label;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.siphonlab.ago.Slots;
import org.siphonlab.ago.opcode.Jump;
import org.siphonlab.ago.opcode.arithmetic.Add;
import org.siphonlab.ago.opcode.compare.LittleThan;

import java.lang.invoke.MethodType;

import static org.siphonlab.ago.opcode.Jump.jump_f_B_vc;

public class CompilerTest {

    @Test
    public void slotsProviderTest(){
//        var agoClass = new AgoClass(new MetaClass(), "test",  "Test");
//        var ch = new ClassHeader("test",(byte)1, 1,null,null);
//        var provider = AgoClassLoader.generateSlotsProvider(ch, new SlotDesc[]{
//                new SlotDesc(0, "test1", new TypeDesc(TypeCode.INT, null)),
//                new SlotDesc(1, "test2", new TypeDesc(TypeCode.INT, null)),
//                new SlotDesc(2,  "@v_1", new TypeDesc(TypeCode.INT, null)),
//        });
//        agoClass.setSlotsCreator(provider);
//        Slots slots = agoClass.createSlots();
//
//        StopWatch stopwatch = new StopWatch();
//        stopwatch.start();
//        int sum = 0;
//        int times = 100000000;
//        for (int i = 0; i < times; i++) {
//            sum += i;
//        }
//        stopwatch.stop();
//        System.out.println("exhaust " + stopwatch.getNanoTime());       // exhaust 9835300    total:887459712, in netcore8, release, 900
//        System.out.println(sum);

        //slots = agoClass.getSlotsProvider().provide();
        var slots = new Slots() {
            int i0;
            int i1;
            int i2;
            boolean b3;
            @Override
            public int getInt(int slot) {
                switch (slot){
                    case 0 : return i0;
                    case 1 : return i1;
                    case 2 : return i2;
                    default: throw new UnsupportedOperationException();
                }
            }

            @Override
            public boolean getBoolean(int slot) {
                switch (slot){
                    case 3 : return b3;
                    default: throw new UnsupportedOperationException();
                }
            }

            @Override
            public void setInt(int slot, int value) {
                switch (slot){
                    case 0: i0 = value; return;
                    case 1: i1 = value; return;
                    case 2: i2 = value; return;
                }
                throw new UnsupportedOperationException();
            }

            @Override
            public void setBoolean(int slot, boolean value) {
                switch (slot){
                    case 3: b3 = value; return;
                }
                throw new UnsupportedOperationException();
            }

            @Override
            public void incInt(int slot, int value) {
                switch (slot){
                    case 0: i0 += value; return;
                    case 1: i1 += value; return;
                    case 2: i2 += value; return;
                }
            }
        };
        pureSlotTest(slots);

        classMakerTest(slots);
        // in asus laptop 4900H, jdk22.0.2+9.1:
        //      pure java   slots+java   class maker for + slots    simulate interpreter core       optimized interpreter
        //      37651900    75776500     84394300(2.2times)         1496988100(39.76 times)         1427587000
        // at the same level with simulate interpreter core
        interpreterTest(slots);
    }

    private static void pureSlotTest(Slots slots) {
        StopWatch stopwatch;
        stopwatch = new StopWatch();
        stopwatch.start();
        slots.setInt(2,100000000);
        while(slots.getInt(1) < slots.getInt(2)) {
            slots.setInt(0, slots.getInt(0) + slots.getInt(1));
            slots.setInt(1, slots.getInt(1) + 1);
        }
        stopwatch.stop();
        System.out.println("exhaust " + stopwatch.getNanoTime());       // exhaust 72635800   total:887459712 , 6.811x slower than above, in netcore8, release: 219298800,1152651100, debug:2802578200
        System.out.println(slots.getInt(0));
    }

    private static void interpreterTest(Slots inSlots) {
        var r = new Runnable() {
            Slots slots = inSlots;

            @Override
            public void run() {
                StopWatch stopwatch;
                /*
                    10	const_i_vc	0,0
                    13	const_i_vc	1,100
                    16	const_i_vc	2,0

                    19	lt_i_vvv	3,2,1
                    23	jump_f_vc	3,34
                    26	add_i_vv	0,2
                    29	add_i_vc	2,1
                    32	jump_c	19
                 */
                final int lt_i_vvv = LittleThan.lt_i_vvv;
                final int jump_f_vc = jump_f_B_vc;
                final int add_i_vv = Add.add_i_vv;
                final int add_i_vc = Add.add_i_vc;
                final int jump_c = Jump.jump_c;
                IntArrayList codeLs = new IntArrayList();
                /*0*/
                codeLs.addInt(lt_i_vvv);
                /*1*/
                codeLs.addInt(3);
                /*2*/
                codeLs.addInt(2);
                /*3*/
                codeLs.addInt(1);
                /*4*/
                codeLs.addInt(jump_f_vc);
                /*5*/
                codeLs.addInt(3);
                /*6*/
                codeLs.addInt(1000);
                /*7*/
                codeLs.addInt(add_i_vv);
                /*8*/
                codeLs.addInt(0);
                /*9*/
                codeLs.addInt(2);
                /*10*/
                codeLs.addInt(add_i_vc);
                /*11*/
                codeLs.addInt(2);
                /*12*/
                codeLs.addInt(1);
                /*15*/
                codeLs.addInt(jump_c);
                /*16*/
                codeLs.addInt(0);
                var code = codeLs.toIntArray();

                slots.setInt(0, 0);
                slots.setInt(1, 100000000);
                slots.setInt(2, 0);
                stopwatch = new StopWatch();
                stopwatch.start();
                for (int pc = 0; pc < code.length; ) {
                    switch (code[pc++]) {
                        case lt_i_vvv:
                            slots.setBoolean(code[pc++], slots.getInt(code[pc++]) < slots.getInt(code[pc++]));
                            break;
                        case jump_f_B_vc:
                            if (!slots.getBoolean(code[pc++])) pc = code[pc++];
                            else pc++;
                            break;
                        case add_i_vv:
                            slots.incInt(code[pc++], slots.getInt(code[pc++]));
                            break;
                        case add_i_vc:
                            slots.incInt(code[pc++], code[pc++]);
                            break;
                        case jump_c:
                            pc = code[pc++];
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + code[pc]);
                    }
                }
                stopwatch.stop();
                System.out.println("exhaust " + stopwatch.getNanoTime());       // exhaust 76750300
                System.out.println(slots.getInt(0));
            }
        };
        r.run();
    }

    private static void classMakerTest(Slots slots) {
        StopWatch stopwatch;
        slots.setInt(0,0);
        slots.setInt(1,0);
        slots.setInt(2,100000000);
        var cm = ClassMaker.begin().public_().implement(Runnable.class);
        cm.addConstructor().public_();
        cm.addField(Slots.class, "slots").public_();
        var run = cm.addMethod("run", MethodType.methodType(void.class)).public_().override();
        var fld = run.field("slots");
        Label start = run.label().here();
        Label end = run.label();
        fld.invoke("getInt", 1).ifGe(fld.invoke("getInt",2), end);
        fld.invoke("setInt", 0, fld.invoke("getInt",0).add(fld.invoke("getInt", 1)));
        fld.invoke("incInt", 1, 1);
        run.goto_(start);
        end.here();
        run.return_();
        var cls = cm.finish();
        try {
            Runnable runnable = (Runnable) cls.newInstance();
            FieldUtils.getField(cls,"slots").set(runnable, slots);
            stopwatch = new StopWatch();
            stopwatch.start();
            runnable.run();
            stopwatch.stop();
            System.out.println("exhaust " + stopwatch.getNanoTime());       // exhaust 76750300
            System.out.println(slots.getInt(0));
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


}
