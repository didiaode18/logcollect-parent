package org.apache.logcollect.rpc;

import java.io.IOException;

import org.apache.hadoop.ipc.VersionedProtocol;

public interface ClientProtocol extends VersionedProtocol {
	public static final long versionID = 1L;
	String sendHeartbeat(String value) throws IOException;
}
