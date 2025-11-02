package org.siphonlab.ago.runtime.vertx;

import org.siphonlab.ago.AgoEngine;
import org.siphonlab.ago.AgoRunSpace;
import org.siphonlab.ago.RunSpaceHost;
import org.siphonlab.ago.runtime.rdb.RdbAgoSpace;

public class VertXRunSpace extends RdbAgoSpace {

    public VertXRunSpace(AgoEngine agoEngine, RunSpaceHost runSpaceHost) {
        super(agoEngine, runSpaceHost);

    }

}
