package com.soffid.addons.bpm.web.mxgraph;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.servlet.ServletContext;

import org.zkoss.web.fn.ServletFns;
import org.zkoss.web.servlet.xel.RequestContext;

public class ZkdbFns {
	public static java.lang.String includeImage(java.lang.String s) throws IOException {
		ServletContext sctx = ServletFns.getCurrentServletContext();
		InputStream in = sctx.getResourceAsStream(s);
		if (in == null)
			return "";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte b[] = new byte [8192];
		for (int read = in.read(b); read >= 0; read = in.read(b))
			out.write(b, 0, read);
		return "data:image/svg+xml,"+URLEncoder.encode(out.toString("UTF-8"), "UTF-8").replace("+", "%20");
	}
}
