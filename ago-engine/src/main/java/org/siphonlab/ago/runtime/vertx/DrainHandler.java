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
package org.siphonlab.ago.runtime.vertx;

import io.vertx.core.streams.WriteStream;
import org.siphonlab.ago.native_.NativeFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrainHandler {
    private static Logger LOGGER = LoggerFactory.getLogger(DrainHandler.class);

    private boolean draining = false;
    private NativeFrame waitDrain;

    public DrainHandler(WriteStream<?> writeStream) {
        writeStream.drainHandler(_ -> {
            draining = true;
            if(LOGGER.isDebugEnabled()) LOGGER.debug("got drain request, while waitDrain is "+ (waitDrain == null ? "null" : "not null"));
            if(waitDrain != null){
                draining = false;
                waitDrain.finishVoidAsync();
                waitDrain = null;
            }
        });
    }

    public void connect(NativeFrame waitDrain) {
        if(draining) {
            if(LOGGER.isDebugEnabled()) LOGGER.debug("draining, direct return");
            draining = false;
            waitDrain.finishVoid();
        } else {
            if(LOGGER.isDebugEnabled()) LOGGER.debug("wait drain come in");
            if(this.waitDrain != null) {
                System.out.println(1);
            }
            this.waitDrain = waitDrain;
            waitDrain.beginAsync();
        }
    }

}
