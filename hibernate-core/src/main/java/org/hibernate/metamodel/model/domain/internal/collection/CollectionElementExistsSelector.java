/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;

/**
 * @author Andrea Boriero
 */
public class CollectionElementExistsSelector extends AbstractSelector {

	public CollectionElementExistsSelector(
			PersistentCollectionDescriptor collectionDescriptor,
			Table table,
			String sqlWhereString,
			SessionFactoryImplementor sessionFactory) {
		super( collectionDescriptor, table, sqlWhereString, sessionFactory );
	}

	public Boolean elementExists(
			Object loadedKey,
			Object element,
			PersistentCollection collection,
			SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();

		bindCollectionKey( loadedKey, jdbcParameterBindings, session );
		bindCollectionElement( element, collection, jdbcParameterBindings, session );

		final List results = execute( jdbcParameterBindings, session );
		return !results.isEmpty();
	}

	@Override
	protected void applySqlSelections(
			QuerySpec querySpec,
			TableGroup tableGroup,
			SelectClause selectClause,
			Consumer<DomainResult> domainResultsCollector,
			DomainResultCreationState creationState) {
		final SqlExpressableType sqlExpressableType = IntegerSqlDescriptor.INSTANCE.getSqlExpressableType(
				IntegerJavaDescriptor.INSTANCE,
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
		final SqlSelection sqlSelection = new SqlSelectionImpl(
				1,
				0,
				new QueryLiteral( 1, sqlExpressableType, Clause.SELECT ),
				sqlExpressableType
		);

		selectClause.addSqlSelection( sqlSelection );

		domainResultsCollector.accept( new BasicResultImpl( null, sqlSelection, sqlExpressableType ) );
	}

	@Override
	protected void applyPredicates(
			Junction junction,
			TableGroup tableGroup,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SqlAstCreationState creationState) {
		applyPredicates(
				junction,
				getCollectionDescriptor().getCollectionKeyDescriptor(),
				tableGroup,
				columnCollector,
				creationState
		);

		applyPredicates(
				junction,
				getCollectionDescriptor().getElementDescriptor(),
				tableGroup,
				columnCollector,
				creationState
		);
	}
}
