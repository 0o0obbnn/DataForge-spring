package com.dataforge.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.dataforge.cli.commands.GenerateCommand;

import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

/** DataForge CLI应用程序启动类。 */
@SpringBootApplication(scanBasePackages = {"com.dataforge"})
public class DataForgeCliApplication implements CommandLineRunner {

  private final ApplicationContext applicationContext;

  public DataForgeCliApplication(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public static void main(String[] args) {
    System.setProperty("spring.main.web-application-type", "none");
    System.exit(SpringApplication.exit(SpringApplication.run(DataForgeCliApplication.class, args)));
  }

  @Override
  public void run(String... args) throws Exception {
    PicocliSpringFactory factory = new PicocliSpringFactory(applicationContext);
    CommandLine cmd = new CommandLine(GenerateCommand.class, factory);

    cmd.setCommandName("dataforge");
    int exitCode = cmd.execute(args);
    System.exit(exitCode);
  }
}
