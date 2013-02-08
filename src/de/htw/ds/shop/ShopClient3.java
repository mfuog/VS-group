package de.htw.ds.shop;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.xml.ws.Service;
import de.sb.javase.TypeMetadata;
import de.sb.javase.xml.Namespaces;


/**
 * <p>Swing based GUI-client utilizing the JAX-WS shop service.</p>
 */
@TypeMetadata(copyright="2012-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ShopClient3 {

	/**
	 * Application entry point. The given runtime parameters must be a SOAP service URI.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given URI is malformed
	 * @throws MalformedURLException if the given URI cannot be converted into a URL
	 */
	public static void main(final String[] args) throws URISyntaxException, MalformedURLException {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (final Exception exception) {
			// do nothing
		}

		final URL wsdlLocator = new URL(args[0] + "?wsdl");
		final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(ShopService.class));
		final ShopService proxy = proxyFactory.getPort(ShopService.class);

		final JFrame frame = new JFrame("Dynamic (bottom-up) JAX-WS Shop Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(new ShopPane(proxy));
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
}