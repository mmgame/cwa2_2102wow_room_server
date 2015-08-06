/**
 * 
 */

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.cwa.service.ServerService;

/**
 * @author mali
 * 
 */
public class StartRoomServer {
	private static final Logger log = LoggerFactory.getLogger(StartRoomServer.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			DOMConfigurator.configureAndWatch(args[0]);

			Resource[] props = new FileSystemResource[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				props[i - 1] = new FileSystemResource(args[i]);
			}
			PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
			ppc.setFileEncoding("UTF-8");
			ppc.setLocations(props);

			GenericApplicationContext ctx = new GenericApplicationContext();
			XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
			xmlReader.loadBeanDefinitions(new FileSystemResource("config/root.xml"));
			ppc.postProcessBeanFactory((ConfigurableListableBeanFactory) ctx.getDefaultListableBeanFactory());
			ctx.refresh();

			Runtime.getRuntime().addShutdownHook(shutdownHook(ctx));
			log.info("启动完毕!");

			// 启动server
			startserver(ctx);
		} catch (Exception e) {
			log.error("启动失败! ", e);
		}
	}

	private static void startserver(GenericApplicationContext ctx) throws Exception {
		ServerService serverService = (ServerService) ctx.getBean("serverService");
		serverService.startup();
	}

	private static Thread shutdownHook(final GenericApplicationContext ctx) {
		return new Thread() {
			@Override
			public void run() {
				ServerService serverService = (ServerService) ctx.getBean("serverService");
				serverService.shutdown();
				log.warn("正常关闭");
			}
		};
	}
}
