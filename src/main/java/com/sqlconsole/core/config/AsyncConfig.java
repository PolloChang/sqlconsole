package com.sqlconsole.core.config;

import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for asynchronous task execution.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {


    /**
   * Creates a task executor for export tasks using virtual threads.
   *
   * @return the task executor
   */
  @Bean(name = "exportTaskExecutor")
  public TaskExecutor exportTaskExecutor() {
    return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
  }
}
