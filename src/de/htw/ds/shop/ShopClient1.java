package de.htw.ds.shop;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.ws.Service;
import de.sb.javase.TypeMetadata;
import de.sb.javase.xml.Namespaces;


/**
 * <p>Non-interactive command line client utilizing the JAX-WS shop service.
 * Note that the client terminates after each service method call.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ShopClient1 {
	private static enum Command {
		QUERY_ARTICLES,
		QUERY_ARTICLE,
		REGISTER_CUSTOMER,
		UNREGISTER_CUSTOMER,
		QUERY_CUSTOMER,
		CREATE_ORDER,
		CANCEL_ORDER,
		QUERY_ORDER,
		QUERY_ORDERS
	}


	/**
	 * Application entry point. The given runtime parameters must be a service URI,
	 * the command, and subsequently the command parameters.
	 * @param args the given runtime arguments
	 * @throws Exception if there is a problem
	 */
	public static void main(final String[] args) throws Exception {
		final Command command = Command.valueOf(args[1].toUpperCase());

		final URL wsdlLocator = new URL(args[0] + "?wsdl");
		final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(ShopService.class));
		final ShopService proxy = proxyFactory.getPort(ShopService.class);
		System.out.println("Dynamic (bottom-up) JAX-WS Shop Client");

		switch(command) {
			case QUERY_ARTICLES: {
				final Collection<Article> articles = proxy.queryArticles();
				System.out.println("List of articles:");
				for (final Article article : articles) {
					System.out.format("article-ID=%s, description=%s, available-units=%s, unit-price=%.2f€.\n", article.getIdentity(), article.getDescription(), article.getCount(), 0.01 * article.getPrice());
				}
				break;
			}
			case QUERY_ARTICLE: {
				final long articleIdentity = Long.parseLong(args[2]);
				final Article article = proxy.queryArticle(articleIdentity);
				System.out.format("Article %s.\n", articleIdentity);
				System.out.format("description=%s, available-units=%s, unit-price=%.2f€.\n", article.getDescription(), article.getCount(), 0.01 * article.getPrice());
				break;
			}
			case REGISTER_CUSTOMER: {
				final Customer customer = new Customer();
				customer.setAlias(args[2]);
				customer.setPassword(args[3]);
				customer.setFirstName(args[4]);
				customer.setLastName(args[5]);
				customer.setStreet(args[6]);
				customer.setPostcode(args[7]);
				customer.setCity(args[8]);
				customer.setEmail(args[9]);
				customer.setPhone(args[10]);
				final long customerIdentity = proxy.registerCustomer(customer);
				System.out.format("Customer created, identity=%s.\n", customerIdentity);
				break;
			}
			case UNREGISTER_CUSTOMER: {
				final String alias = args[2];
				final String password = args[3];
				final long customerIdentity = proxy.unregisterCustomer(alias, password);
				System.out.format("Customer removed, identity=%s.\n", customerIdentity);
				break;
			}
			case QUERY_CUSTOMER: {
				final String alias = args[2];
				final String password = args[3];
				final Customer customer = proxy.queryCustomer(alias, password);
				System.out.format("Customer %s:\n", customer.getIdentity());
				System.out.format("first-name=%s, last-name=%s, street=%s, post-code=%s, city=%s, eMail=%s, phone=%s.\n", customer.getFirstName(), customer.getLastName(), customer.getStreet(), customer.getPostcode(), customer.getCity(), customer.getEmail(), customer.getPhone());
				break;
			}
			case CREATE_ORDER: {
				final String alias = args[2];
				final String password = args[3];
				final Set<OrderItem> items = new HashSet<OrderItem>();
				for (int index = 4; index < args.length; ++index) {
					final String[] data = args[index].split("-");
					final OrderItem item = new OrderItem();
					item.setArticleIdentity(Long.parseLong(data[0]));
					item.setArticleGrossPrice(Math.round(100 * Double.parseDouble(data[1])));
					item.setCount(Integer.parseInt(data[2]));
					items.add(item);
				}
				final long orderIdentity = proxy.createOrder(alias, password, items);
				System.out.format("Order created, identity=%s.\n", orderIdentity);
				break;
			}
			case CANCEL_ORDER: {
				final String alias = args[2];
				final String password = args[3];
				final long orderIdentity = Long.parseLong(args[4]);
				proxy.cancelOrder(alias, password, orderIdentity);
				System.out.format("Order canceled, identity=%s.\n", orderIdentity);
				break;
			}
			case QUERY_ORDER: {
				final String alias = args[2];
				final String password = args[3];
				final long orderIdentity = Long.parseLong(args[4]);
				final Order order = proxy.queryOrder(alias, password, orderIdentity);
				System.out.format("Order %s:\n", orderIdentity);
				System.out.format("creation=%1$tF %tT, taxRate=%.2f%%, gross=%.2f€, net=%.2f€.\n", order.getCreationTimestamp(), 100 * order.getTaxRate(), 0.01 * order.getGrossPrice(), 0.01 * order.getNetPrice());
				for (final OrderItem item : order.getItems()) {
					System.out.format("article-ID=%s, unit-gross-price=%.2f€, units=%s.\n", item.getArticleIdentity(), 0.01 * item.getArticleGrossPrice(), item.getCount());
				}
				break;
			}
			case QUERY_ORDERS: {
				final String alias = args[2];
				final String password = args[3];
				final Collection<Order> orders = proxy.queryOrders(alias, password);
				for (final Order order : orders) {
					System.out.format("Order %s:\n", order.getIdentity());
					System.out.format("creation=%1$tF %tT, taxRate=%.2f%%, gross=%.2f€, net=%.2f€.\n", order.getCreationTimestamp(), 100 * order.getTaxRate(), 0.01 * order.getGrossPrice(), 0.01 * order.getNetPrice());
					for (final OrderItem item : order.getItems()) {
						System.out.format("article-ID=%s, unit-gross-price=%.2f€, units=%s.\n", item.getArticleIdentity(), 0.01 * item.getArticleGrossPrice(), item.getCount());
					}
				}
				break;
			}
		}
	}
}