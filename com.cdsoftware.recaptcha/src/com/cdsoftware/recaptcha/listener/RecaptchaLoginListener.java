/**
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Copyright (C) 2026 www.casadelsoftware.com and contributors (see README.md file).
 */

package com.cdsoftware.recaptcha.listener;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.ShadowElement;
import org.zkoss.zk.ui.util.UiLifeCycle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Script;
import org.zkoss.zul.Html;
import org.zkoss.zhtml.Table;
import org.zkoss.zhtml.Tr;
import org.zkoss.zhtml.Td;
import org.zkoss.zhtml.Div;
import org.adempiere.webui.panel.LoginPanel;
import org.adempiere.webui.window.LoginWindow;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Button;
import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.CLogger;
import org.zkoss.zk.ui.WrongValueException;

/**
 * ZK Listener to dynamically inject Google reCAPTCHA v2 into the iDempiere login screen.
 * Handles client-side token fetching and server-side token validation.
 * 
 * System Configurator (Data Dictionary) Configuration:
 * 1. Open "System Configurator" window in iDempiere.
 * 2. Configure the following variables:
 *    - RECAPTCHA_ENABLED: Set to 'Y' to enable (default 'N').
 *    - RECAPTCHA_SITE_KEY: Your public Google reCAPTCHA v2 Site Key.
 *    - RECAPTCHA_SECRET_KEY: Your private Google reCAPTCHA v2 Secret Key.
 * 
 * @author  Carl0jgr
 */
public class RecaptchaLoginListener implements UiLifeCycle {

	private static final CLogger log = CLogger.getCLogger(RecaptchaLoginListener.class);

	@Override
	public void afterComponentAttached(Component comp, Page page) {
		if (comp instanceof LoginPanel) {
			LoginPanel loginPanel = (LoginPanel) comp;
			setupRecaptcha(loginPanel);
		}
	}

	private void setupRecaptcha(LoginPanel loginPanel) {
		boolean enabled = MSysConfig.getBooleanValue("RECAPTCHA_ENABLED", false);
		//System.out.println("leyó la variable?: "+ enabled);
		if (!enabled) {
			return;
		}

		String siteKey = MSysConfig.getValue("RECAPTCHA_SITE_KEY", "");
		if (siteKey == null || siteKey.trim().isEmpty()) {
			log.warning("reCAPTCHA is enabled but RECAPTCHA_SITE_KEY is not configured.");
			return;
		}

		// 1. Inject the reCAPTCHA script
		Script script = new Script();
		script.setSrc("https://www.google.com/recaptcha/api.js");
		loginPanel.appendChild(script);

		// 2. Add hidden Textbox to hold token
		Textbox recaptchaToken = new Textbox();
		recaptchaToken.setId("recaptchaToken");
		recaptchaToken.setStyle("display: none;");
		loginPanel.appendChild(recaptchaToken);

		// 3. Find the grdLogin table and insert reCAPTCHA widget
		Table grdLogin = (Table) loginPanel.getFellowIfAny("grdLogin");
		if (grdLogin != null) {
			Tr tr = new Tr();
			tr.setId("rowRecaptcha");
			
			// Column 1: Placeholder / empty cell to align with labels
			Td tdLabel = new Td();
			tdLabel.setSclass("login-label");
			tr.appendChild(tdLabel);
			
			// Column 2: Contains the reCAPTCHA widget aligning it with the input fields
			Td tdContent = new Td();
			tdContent.setSclass("login-field");
			tdContent.setStyle("padding: 10px 0;");
			
			Html html = new Html("<div style=\"display: inline-block; width: 211px; height: 53px; overflow: hidden; vertical-align: middle; border-radius: 4px;\">"
					+ "<div class=\"g-recaptcha\" data-sitekey=\"" + siteKey + "\" style=\"transform: scale(0.70); -webkit-transform: scale(0.70); transform-origin: 0 0; -webkit-transform-origin: 0 0;\"></div>"
					+ "</div>");
			tdContent.appendChild(html);
			tr.appendChild(tdContent);
			
			grdLogin.appendChild(tr);
		}

		// 4. Update the OK button click behaviour
		ConfirmPanel confirmPanel = (ConfirmPanel) loginPanel.getFellowIfAny("confirmPanel");
		if (confirmPanel != null) {
			Button okBtn = confirmPanel.getButton(ConfirmPanel.A_OK);
			if (okBtn != null) {
				// Set client-side listener to retrieve the token and store it in hidden Textbox
				String clientScript = "var token = grecaptcha.getResponse(); "
				                    + "var txt = zk.$('$" + recaptchaToken.getId() + "'); "
				                    + "if (txt) { txt.setValue(token); txt.fireOnChange(); } "
				                    + "zAu.cmd0.showBusy(null);";
				okBtn.setWidgetListener("onClick", clientScript);

				// Add server-side listener (priority 1000) to validate the token before LoginPanel's listener
				okBtn.addEventListener(1000, Events.ON_CLICK, new EventListener<Event>() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onEvent(Event event) throws Exception {
						String token = recaptchaToken.getValue();
						if (!validateRecaptcha(token)) {
							event.stopPropagation();
							Clients.clearBusy();
							Clients.evalJavaScript("grecaptcha.reset();");
							throw new WrongValueException(okBtn, getValidationErrorMessage());
						}
					}
				});
			}
		}

		// 5. Intercept LoginWindow ENTER key submission
		Component parent = loginPanel.getParent();
		if (parent instanceof LoginWindow) {
			LoginWindow wndLogin = (LoginWindow) parent;
			
			// Set client-side listener on LoginWindow to capture ENTER and sync token
			String clientWindowScript = "var token = grecaptcha.getResponse(); "
			                          + "var txt = zk.$('$" + recaptchaToken.getId() + "'); "
			                          + "if (txt) { txt.setValue(token); txt.fireOnChange(); } "
			                          + "zAu.cmd0.showBusy(null);";
			wndLogin.setWidgetListener("onOK", clientWindowScript);

			// Add server-side listener (priority 1000) to validate the token on ON_OK
			wndLogin.addEventListener(1000, Events.ON_OK, new EventListener<Event>() {
				private static final long serialVersionUID = 1L;

				@Override
				public void onEvent(Event event) throws Exception {
					String token = recaptchaToken.getValue();
					if (!validateRecaptcha(token)) {
						event.stopPropagation();
						Clients.clearBusy();
						Clients.evalJavaScript("grecaptcha.reset();");
						
						// We throw exception on the OK button to highlight it
						Button okBtn = null;
						if (confirmPanel != null) {
							okBtn = confirmPanel.getButton(ConfirmPanel.A_OK);
						}
						throw new WrongValueException(okBtn != null ? okBtn : wndLogin, getValidationErrorMessage());
					}
				}
			});
		}
	}

	private String getValidationErrorMessage() {
		String msg = Msg.getMsg(Env.getCtx(), "RecaptchaValidationError");
		if (msg == null || msg.trim().isEmpty() || "RecaptchaValidationError".equals(msg)) {
			return "reCAPTCHA verification failed. Please try again.";
		}
		return msg;
	}

	private boolean validateRecaptcha(String token) {
		if (token == null || token.trim().isEmpty()) {
			return false;
		}

		String secretKey = MSysConfig.getValue("RECAPTCHA_SECRET_KEY", "");
		if (secretKey == null || secretKey.trim().isEmpty()) {
			log.warning("reCAPTCHA validation failed because RECAPTCHA_SECRET_KEY is not configured.");
			return false;
		}

		try {
			java.net.URL url = new java.net.URL("https://www.google.com/recaptcha/api/siteverify");
			java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(5000); // 5 seconds timeout
			conn.setReadTimeout(5000);    // 5 seconds timeout

			String postParams = "secret=" + java.net.URLEncoder.encode(secretKey, "UTF-8")
			                  + "&response=" + java.net.URLEncoder.encode(token, "UTF-8");

			try (java.io.OutputStream os = conn.getOutputStream()) {
				os.write(postParams.getBytes("UTF-8"));
				os.flush();
			}

			int responseCode = conn.getResponseCode();
			if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
				try (java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
					StringBuilder response = new StringBuilder();
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}

					String json = response.toString();
					return json.contains("\"success\": true");
				}
			} else {
				log.warning("reCAPTCHA validation server returned response code: " + responseCode);
			}
		} catch (Exception e) {
			log.log(java.util.logging.Level.SEVERE, "Error verifying reCAPTCHA token", e);
		}
		return false;
	}

	@Override
	public void afterComponentDetached(Component comp, Page prevpage) {}

	@Override
	public void afterComponentMoved(Component parent, Component child, Component prevparent) {}

	@Override
	public void afterPageAttached(Page page, Desktop desktop) {}

	@Override
	public void afterPageDetached(Page page, Desktop prevdesktop) {}

	@Override
	public void afterShadowAttached(ShadowElement arg0, Component arg1) {}

	@Override
	public void afterShadowDetached(ShadowElement arg0, Component arg1) {}
}
