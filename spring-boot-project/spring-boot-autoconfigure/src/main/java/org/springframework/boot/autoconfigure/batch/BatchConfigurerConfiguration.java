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

import jakarta.persistence.EntityManagerFactory;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provide a {@link BatchConfigurer} according to the current environment.
 *
 * @author Stephane Nicoll
 * @author Andreas Ahlenstorf
 */
@ConditionalOnClass(PlatformTransactionManager.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnMissingBean(BatchConfigurer.class)
@Configuration(proxyBeanMethods = false)
class BatchConfigurerConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = "entityManagerFactory")
	static class JdbcBatchConfiguration {

		@Bean
		BasicBatchConfigurer batchConfigurer(BatchProperties properties, DataSource dataSource,
				@BatchDataSource ObjectProvider<DataSource> batchDataSource,
				ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers,
				@BatchTaskExecutor ObjectProvider<TaskExecutor> batchTaskExecutor) {
			return new BasicBatchConfigurer(properties, batchDataSource.getIfAvailable(() -> dataSource),
					transactionManagerCustomizers.getIfAvailable(), batchTaskExecutor.getIfAvailable());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(EntityManagerFactory.class)
	@ConditionalOnBean(name = "entityManagerFactory")
	static class JpaBatchConfiguration {

		@Bean
		JpaBatchConfigurer batchConfigurer(BatchProperties properties, DataSource dataSource,
				@BatchDataSource ObjectProvider<DataSource> batchDataSource,
				ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers,
				EntityManagerFactory entityManagerFactory,
				@BatchTaskExecutor ObjectProvider<TaskExecutor> batchTaskExecutor) {
			return new JpaBatchConfigurer(properties, batchDataSource.getIfAvailable(() -> dataSource),
					transactionManagerCustomizers.getIfAvailable(), entityManagerFactory,
					batchTaskExecutor.getIfAvailable());
		}

	}

}
