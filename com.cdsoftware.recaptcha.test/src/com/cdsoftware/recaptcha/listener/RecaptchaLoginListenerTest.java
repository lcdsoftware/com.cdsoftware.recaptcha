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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.panel.LoginPanel;
import org.adempiere.webui.window.LoginWindow;
import org.compiere.model.MSysConfig;
import org.compiere.util.CCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zhtml.Table;
import org.zkoss.zhtml.Tr;
import com.cdsoftware.recaptcha.test.util.ReflectionTestUtil;

/**
 * Unit tests for {@link RecaptchaLoginListener}.
 * Uses reflection to mock MSysConfig cache values in order to isolate tests from database calls.
 * 
 * @author Casa del Software
 */
class RecaptchaLoginListenerTest {

	private RecaptchaLoginListener listener;
	private CCache<String, String> sysConfigCache;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() throws Exception {
		listener = new RecaptchaLoginListener();
		
		// 1. Extract static cache of MSysConfig via reflection
		sysConfigCache = (CCache<String, String>) ReflectionTestUtil.getStaticFieldValue(MSysConfig.class, "s_cache");
		sysConfigCache.clear();
		sysConfigCache.put("0_0_ZK_THEME", "iceblue_c");
		sysConfigCache.put("0_0_ZK_BUTTON_STYLE", "I");
		sysConfigCache.put("0_0_MESSAGES_AT_TENANT_LEVEL", "N");

		// 2. Pre-populate Msg caches to avoid DB connection errors
		org.compiere.util.Msg msg = org.compiere.util.Msg.get();
		
		java.lang.reflect.Field langsField = org.compiere.util.Msg.class.getDeclaredField("m_languages");
		langsField.setAccessible(true);
		Map<String, CCache<String, String>> m_languages = (Map<String, CCache<String, String>>) langsField.get(msg);
		m_languages.clear();
		
		CCache<String, String> esMsgCache = new CCache<>("AD_Message", "AD_Message|es", 10, 0, false, 0);
		esMsgCache.put("Ok", "Ok");
		m_languages.put("es", esMsgCache);
		m_languages.put("es_CL", esMsgCache);
		
		CCache<String, String> enMsgCache = new CCache<>("AD_Message", "AD_Message|en_US", 10, 0, false, 0);
		enMsgCache.put("Ok", "Ok");
		m_languages.put("en", enMsgCache);
		m_languages.put("en_US", enMsgCache);

		java.lang.reflect.Field elemField = org.compiere.util.Msg.class.getDeclaredField("m_elementNameCache");
		elemField.setAccessible(true);
		Map<String, CCache<String, String>> m_elementNameCache = (Map<String, CCache<String, String>>) elemField.get(msg);
		m_elementNameCache.clear();

		CCache<String, String> esElemCache = new CCache<>("AD_Element", "AD_Element|es", 10, 0, false, 0);
		esElemCache.put("Ok|true", "Ok");
		esElemCache.put("Ok|false", "Ok");
		m_elementNameCache.put("es", esElemCache);
		m_elementNameCache.put("es_CL", esElemCache);

		CCache<String, String> enElemCache = new CCache<>("AD_Element", "AD_Element|en_US", 10, 0, false, 0);
		enElemCache.put("Ok|true", "Ok");
		enElemCache.put("Ok|false", "Ok");
		m_elementNameCache.put("en", enElemCache);
		m_elementNameCache.put("en_US", enElemCache);
	}

	@Test
	void shouldDoNothingWhenDisabled() {
		// Arrange
		sysConfigCache.put("0_0_RECAPTCHA_ENABLED", "N");
		LoginPanel loginPanel = mock(LoginPanel.class);
		Page page = mock(Page.class);

		// Act
		listener.afterComponentAttached(loginPanel, page);

		// Assert
		verify(loginPanel, never()).appendChild(any(Component.class));
	}

	@Test
	void shouldDoNothingWhenSiteKeyIsEmpty() {
		// Arrange
		sysConfigCache.put("0_0_RECAPTCHA_ENABLED", "Y");
		sysConfigCache.put("0_0_RECAPTCHA_SITE_KEY", "");
		LoginPanel loginPanel = mock(LoginPanel.class);
		Page page = mock(Page.class);

		// Act
		listener.afterComponentAttached(loginPanel, page);

		// Assert
		verify(loginPanel, never()).appendChild(any(Component.class));
	}

	@Test
	void shouldInjectComponentsWhenEnabledAndSiteKeyConfigured() {
		// Arrange
		sysConfigCache.put("0_0_RECAPTCHA_ENABLED", "Y");
		sysConfigCache.put("0_0_RECAPTCHA_SITE_KEY", "my-test-site-key");

		LoginPanel loginPanel = mock(LoginPanel.class);
		Page page = mock(Page.class);
		Table grdLogin = mock(Table.class);
		
		// Use real ConfirmPanel and get the real Button from it
		ConfirmPanel confirmPanel = new ConfirmPanel(false);
		Button okBtn = confirmPanel.getButton(ConfirmPanel.A_OK);
		assertNotNull(okBtn);

		// Use a real LoginWindow instance since it is not final and has an empty constructor
		LoginWindow loginWindow = new LoginWindow();

		// Stub ZK component tree dependencies
		when(loginPanel.getFellowIfAny("grdLogin")).thenReturn(grdLogin);
		when(loginPanel.getFellowIfAny("confirmPanel")).thenReturn(confirmPanel);
		when(loginPanel.getParent()).thenReturn(loginWindow);

		// Act
		listener.afterComponentAttached(loginPanel, page);

		// Assert
		// 1. Verify reCAPTCHA script and hidden textbox are appended to LoginPanel
		verify(loginPanel, times(2)).appendChild(any(Component.class));

		// 2. Verify reCAPTCHA Row is appended to the grdLogin table
		verify(grdLogin).appendChild(any(Tr.class));

		// 3. Verify OK Button client-side and server-side event listeners are added
		String okClientScript = okBtn.getWidgetListener("onClick");
		assertNotNull(okClientScript);
		assertTrue(okClientScript.contains("grecaptcha.getResponse()"));

		Iterable<EventListener<? extends Event>> okListeners = okBtn.getEventListeners(Events.ON_CLICK);
		assertNotNull(okListeners);
		assertTrue(okListeners.iterator().hasNext());

		// 4. Verify LoginWindow client-side and server-side event listeners are added
		String wndClientScript = loginWindow.getWidgetListener("onOK");
		assertNotNull(wndClientScript);
		assertTrue(wndClientScript.contains("grecaptcha.getResponse()"));

		Iterable<EventListener<? extends Event>> wndListeners = loginWindow.getEventListeners(Events.ON_OK);
		assertNotNull(wndListeners);
		assertTrue(wndListeners.iterator().hasNext());
	}

	@Test
	void testInstantiation() {
		assertNotNull(listener);
	}
}
