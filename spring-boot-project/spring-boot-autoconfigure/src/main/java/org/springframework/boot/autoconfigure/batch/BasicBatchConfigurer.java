/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.batch;

import javax.sql.DataSource;

import org.h2.util.Task;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Basic {@link BatchConfigurer} implementation.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 * @since 1.0.0
 */
public class BasicBatchConfigurer implements BatchConfigurer, InitializingBean {

	private final BatchProperties properties;

	private final DataSource dataSource;

	private PlatformTransactionManager transactionManager;

	private final TransactionManagerCustomizers transactionManagerCustomizers;

	private final TaskExecutor taskExecutor;

	private JobRepository jobRepository;

	private JobLauncher jobLauncher;

	private JobExplorer jobExplorer;

	/**
	 * Create a new {@link BasicBatchConfigurer} instance.
	 * @param properties the batch properties
	 * @param dataSource the underlying data source
	 * @param transactionManagerCustomizers transaction manager customizers (or
	 * {@code null})
	 * @deprecated since 3.0.0 for removal in 3.2.0 in favor of
	 * {@link #BasicBatchConfigurer(BatchProperties, DataSource, TransactionManagerCustomizers, TaskExecutor)}
	 */
	@Deprecated
	protected BasicBatchConfigurer(BatchProperties properties, DataSource dataSource,
			TransactionManagerCustomizers transactionManagerCustomizers) {
		this(properties, dataSource, transactionManagerCustomizers, null);
	}

	/**
	 * Create a new {@link BasicBatchConfigurer} instance.
	 * @param properties the batch properties
	 * @param dataSource the underlying data source
	 * @param transactionManagerCustomizers transaction manager customizers (or
	 * {@code null})
	 * @param taskExecutor the executor to be used by {@link JobLauncher} (or {@code null})
	 */
	protected BasicBatchConfigurer(BatchProperties properties, DataSource dataSource,
			TransactionManagerCustomizers transactionManagerCustomizers, TaskExecutor taskExecutor) {
		this.properties = properties;
		this.dataSource = dataSource;
		this.transactionManagerCustomizers = transactionManagerCustomizers;
		this.taskExecutor = taskExecutor;
	}

	@Override
	public JobRepository getJobRepository() {
		return this.jobRepository;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public JobLauncher getJobLauncher() {
		return this.jobLauncher;
	}

	@Override
	public JobExplorer getJobExplorer() throws Exception {
		return this.jobExplorer;
	}

	@Override
	public void afterPropertiesSet() {
		initialize();
	}

	public void initialize() {
		try {
			this.transactionManager = buildTransactionManager();
			this.jobRepository = createJobRepository();
			this.jobLauncher = createJobLauncher();
			this.jobExplorer = createJobExplorer();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to initialize Spring Batch", ex);
		}
	}

	protected JobExplorer createJobExplorer() throws Exception {
		PropertyMapper map = PropertyMapper.get();
		JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
		factory.setDataSource(this.dataSource);
		map.from(this.properties.getJdbc()::getTablePrefix).whenHasText().to(factory::setTablePrefix);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	protected JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(getJobRepository());
		if (this.taskExecutor != null) {
			jobLauncher.setTaskExecutor(this.taskExecutor);
		}
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	protected JobRepository createJobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		PropertyMapper map = PropertyMapper.get();
		map.from(this.dataSource).to(factory::setDataSource);
		map.from(this::determineIsolationLevel).whenNonNull().to(factory::setIsolationLevelForCreate);
		map.from(this.properties.getJdbc()::getTablePrefix).whenHasText().to(factory::setTablePrefix);
		map.from(this::getTransactionManager).to(factory::setTransactionManager);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	/**
	 * Determine the isolation level for create* operation of the {@link JobRepository}.
	 * @return the isolation level or {@code null} to use the default
	 */
	protected String determineIsolationLevel() {
		return null;
	}

	protected PlatformTransactionManager createTransactionManager() {
		return new DataSourceTransactionManager(this.dataSource);
	}

	private PlatformTransactionManager buildTransactionManager() {
		PlatformTransactionManager transactionManager = createTransactionManager();
		if (this.transactionManagerCustomizers != null) {
			this.transactionManagerCustomizers.customize(transactionManager);
		}
		return transactionManager;
	}

}
