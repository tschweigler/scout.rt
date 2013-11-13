package org.eclipse.scout.cloud.sessionstore;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.spy.memcached.MemcachedClient;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.http.servletfilter.session.AbstractSessionStoreService;

public class MemcachedSessionService extends AbstractSessionStoreService {

	private static final IScoutLogger LOG = ScoutLogManager.getLogger(MemcachedSessionService.class);
	private MemcachedClient client;
	private String configEndpoint = "localhost";
	private Integer clusterPort = 11211;
	private Integer cacheTime = 3600;

	public MemcachedSessionService() {
		super();

		try {
			client = new MemcachedClient(new InetSocketAddress(configEndpoint, clusterPort));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Object getAttribute(HttpServletRequest req, HttpServletResponse res, String key) {
		String str = (String) client.get(getSessionId(req, res) + "_" + key);
		if (str != null) {
			return deserialize(StringUtility.hexToBytes(str));
		} else {
			LOG.info("No value in cache for " + key);
		}

		return null;
	}

	public void setAttribute(HttpServletRequest req, HttpServletResponse res, String key, Object value) {
		client.set(getSessionId(req, res) + "_" + key, cacheTime, StringUtility.bytesToHex(serialize(value)));
	}
}
