/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.pp.dsl;

import org.eclipse.xtext.conversion.IValueConverterService;
import org.eclipse.xtext.formatting.IIndentationInformation;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.parser.antlr.Lexer;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.serializer.sequencer.IHiddenTokenSequencer;
import org.eclipse.xtext.validation.CompositeEValidator;

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.puppetlabs.geppetto.common.tracer.AbstractTracer.DefaultStringProvider;
import com.puppetlabs.geppetto.common.tracer.DefaultTracer;
import com.puppetlabs.geppetto.common.tracer.IStringProvider;
import com.puppetlabs.geppetto.common.tracer.ITracer;
import com.puppetlabs.geppetto.pp.dsl.formatting.PPCommentConfiguration;
import com.puppetlabs.geppetto.pp.dsl.formatting.PPSemanticLayout;
import com.puppetlabs.geppetto.pp.dsl.formatting.PPStylesheetProvider;
import com.puppetlabs.geppetto.pp.dsl.lexer.PPOverridingLexer;
import com.puppetlabs.geppetto.pp.dsl.linking.PPLinker;
import com.puppetlabs.geppetto.pp.dsl.linking.PPQualifiedNameConverter;
import com.puppetlabs.geppetto.pp.dsl.linking.PPQualifiedNameProvider;
import com.puppetlabs.geppetto.pp.dsl.linking.PPResourceDescriptionManager;
import com.puppetlabs.geppetto.pp.dsl.linking.PPResourceDescriptionStrategy;
import com.puppetlabs.geppetto.pp.dsl.linking.PPResourceServiceProvider;
import com.puppetlabs.geppetto.pp.dsl.linking.PPSearchPath.ISearchPathProvider;
import com.puppetlabs.geppetto.pp.dsl.linking.PPSearchPathProvider;
import com.puppetlabs.geppetto.pp.dsl.ppformatting.PPIndentationInformation;
import com.puppetlabs.geppetto.pp.dsl.serialization.PPValueSerializer;
import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;
import com.puppetlabs.geppetto.pp.dsl.validation.BuiltinTypesModule;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationAdvisorProvider;
import com.puppetlabs.xtext.dommodel.DomModelUtils;
import com.puppetlabs.xtext.dommodel.formatter.CSSDomFormatter;
import com.puppetlabs.xtext.dommodel.formatter.FlowLayout;
import com.puppetlabs.xtext.dommodel.formatter.IDomModelFormatter;
import com.puppetlabs.xtext.dommodel.formatter.ILayout;
import com.puppetlabs.xtext.dommodel.formatter.ILayoutManager;
import com.puppetlabs.xtext.dommodel.formatter.comments.ICommentConfiguration;
import com.puppetlabs.xtext.dommodel.formatter.comments.ICommentConfiguration.CommentType;
import com.puppetlabs.xtext.dommodel.formatter.comments.ICommentFormatterAdvice;
import com.puppetlabs.xtext.dommodel.formatter.comments.MoveThenFoldCommentLayout;
import com.puppetlabs.xtext.dommodel.formatter.context.DefaultFormattingContext;
import com.puppetlabs.xtext.dommodel.formatter.context.IFormattingContextFactory;
import com.puppetlabs.xtext.dommodel.formatter.css.DomCSS;
import com.puppetlabs.xtext.dommodel.formatter.css.debug.FormattingTracer;
import com.puppetlabs.xtext.resource.ResourceAccess;
import com.puppetlabs.xtext.resource.ResourceAccessScope;
import com.puppetlabs.xtext.serializer.DomBasedSerializer;

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
public class PPRuntimeModule extends com.puppetlabs.geppetto.pp.dsl.AbstractPPRuntimeModule {

	/**
	 * Binds resource description strategy that binds parent data for inheritance
	 *
	 * @return
	 */
	public Class<? extends IDefaultResourceDescriptionStrategy> bindIDefaultResourceDescriptionStrategy() {
		return PPResourceDescriptionStrategy.class;
	}

	public Class<? extends IFolderDiscriminator> bindIFolderDiscriminator() {
		return FolderDiscriminator.class;
	}

	public Class<? extends IFormattingContextFactory> bindIFormattingContextFactory() {
		return DefaultFormattingContext.Factory.class;
	}

	/**
	 * Handles association of documentation comments, and resource linking (not based on
	 * standard EReference links).
	 */
	@Override
	public Class<? extends org.eclipse.xtext.linking.ILinker> bindILinker() {
		return PPLinker.class;
	}

	/**
	 * Handles FQN <-> String conversion and defines "::" as the separator.
	 */
	public Class<? extends IQualifiedNameConverter> bindIQualifiedNameConverter() {
		return PPQualifiedNameConverter.class;
	}

	/**
	 * Handles creation of QualifiedNames for referenceable PP model elements.
	 *
	 * @see com.puppetlabs.geppetto.pp.dsl.AbstractPPRuntimeModule#bindIQualifiedNameProvider()
	 */
	@Override
	public Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {
		return PPQualifiedNameProvider.class;
	}

	/**
	 * Binds a resource description manager version that provides a resource description
	 * that takes specially imported names into account.
	 *
	 * @return
	 */
	public Class<? extends IResourceDescription.Manager> bindIResourceDescriptionManager() {
		return PPResourceDescriptionManager.class;
	}

	/**
	 * Binds the resource provider that filters unwanted directories
	 */
	public Class<? extends IResourceServiceProvider> bindIResourceServiceProvider() {
		return PPResourceServiceProvider.class;
	}

	@Override
	public Class<? extends ISerializer> bindISerializer() {
		return DomBasedSerializer.class;
	}

	// add transient value serialization service to enable skipping values that are transient from
	// a grammar perspective
	@Override
	public Class<? extends org.eclipse.xtext.parsetree.reconstr.ITransientValueService> bindITransientValueService() {
		return PPGrammarSerialization.class;
	}

	@Override
	public Class<? extends IValueConverterService> bindIValueConverterService() {
		return PPTerminalConverters.class;
	}

	public Class<? extends org.eclipse.xtext.parsetree.reconstr.ITokenSerializer.IValueSerializer> bindIValueSerializer() {
		return PPValueSerializer.class;
	}

	@Override
	public Class<? extends Lexer> bindLexer() {
		return PPOverridingLexer.class;
	}

	public Class<? extends ISearchPathProvider> bindSearchPathProvider() {
		return PPSearchPathProvider.class;
	}

	public void configureBuiltinTypes(com.google.inject.Binder binder) {
		binder.install(new BuiltinTypesModule());
	}

	public void configureDebugTracing(com.google.inject.Binder binder) {
		binder.bind(IStringProvider.class).to(DefaultStringProvider.class);
		binder.bind(ITracer.class).annotatedWith(Names.named(PPDSLConstants.PP_DEBUG_LINKER)).toInstance(
			new DefaultTracer(PPDSLConstants.PP_DEBUG_LINKER));
		binder.bind(ITracer.class).annotatedWith(Names.named(FormattingTracer.DEBUG_FORMATTER)).toInstance(
			new DefaultTracer(FormattingTracer.DEBUG_FORMATTER));
		binder.bind(FormattingTracer.class).asEagerSingleton();
		// DomModelUtils provides debugging formatting and wants access to the singleton FormattingTracer
		binder.requestStaticInjection(DomModelUtils.class);
	}

	/**
	 * for Xtext 2.0 >= M5 see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=322639">Xtext issue 322639</a>
	 */
	public void configureEObjectValidator(com.google.inject.Binder binder) {
		binder.bindConstant().annotatedWith( //
			com.google.inject.name.Names.named(//
			CompositeEValidator.USE_EOBJECT_VALIDATOR)).to(false);
	}

	public void configureFormatting(com.google.inject.Binder binder) {
		binder.bind(IDomModelFormatter.class).to(CSSDomFormatter.class);
		// Want serializer to insert empty WS even if there is no node model
		binder.bind(IHiddenTokenSequencer.class).to(com.puppetlabs.xtext.serializer.acceptor.HiddenTokenSequencer.class);

		// Bind the default style sheet (TODO: Not a nice way to bind impl class - use names)
		binder.bind(DomCSS.class).toProvider(PPStylesheetProvider.class);

		// binder.bind(IIndentationInformation.class).to(IIndentationInformation.Default.class);
		binder.bind(IIndentationInformation.class).to(PPIndentationInformation.class);

		// The layout manager to use by default (when not overridden in style sheet).
		binder.bind(ILayoutManager.class).annotatedWith(Names.named("Default")).to(PPSemanticLayout.class);

		// This binding is good for both headless and ui
		// binder.bind(ICommentConfiguration.class).to(PPCommentConfiguration.class);
		binder.bind(new TypeLiteral<ICommentConfiguration<CommentType>>() {
		}).to(PPCommentConfiguration.class);

		// This binding should be overridden in ui binding with preference and resource specific provider
		binder.bind(ICommentFormatterAdvice.class) //
		.annotatedWith(com.google.inject.name.Names.named(PPCommentConfiguration.SL_FORMATTER_ADVICE_NAME))//
		.toProvider(ICommentFormatterAdvice.DefaultCommentAdviceProvider.class);

		// This binding should be overridden in ui binding with preference and resource specific provider
		binder.bind(ICommentFormatterAdvice.class) //
		.annotatedWith(com.google.inject.name.Names.named(PPCommentConfiguration.ML_FORMATTER_ADVICE_NAME))//
		.toProvider(ICommentFormatterAdvice.DefaultCommentAdviceProvider.class);

		// Bind default layout for all types of comments to MoveThenFold layout
		binder.bind(ILayout.class).annotatedWith(com.google.inject.name.Names.named(FlowLayout.COMMENT_LAYOUT_NAME)) //
		.to(MoveThenFoldCommentLayout.class);
	}

	/**
	 * Configures the {@code ResourceAccessScope}.
	 */
	public void configureResourceContext(com.google.inject.Binder binder) {
		final ResourceAccessScope resourceAccessScope = new ResourceAccessScope();
		binder.bind(ResourceAccessScope.class).toInstance(resourceAccessScope);
		binder.bind(ResourceAccess.class).toProvider(resourceAccessScope);
	}

	// Is now done via an external lexer that is automatically configured into the module
	// contributed by org.eclipse.xtext.generator.parser.antlr.ex.rt.AntlrGeneratorFragment
	@Override
	public void configureRuntimeLexer(com.google.inject.Binder binder) {
		binder.bind(org.eclipse.xtext.parser.antlr.Lexer.class).annotatedWith(
			com.google.inject.name.Names.named(org.eclipse.xtext.parser.antlr.LexerBindings.RUNTIME)).to(PPOverridingLexer.class);
	}

	// contributed by org.eclipse.xtext.generator.parser.antlr.ex.rt.AntlrGeneratorFragment
	public Provider<PPOverridingLexer> providePPOverridingLexer() {
		return org.eclipse.xtext.parser.antlr.LexerProvider.create(PPOverridingLexer.class);
	}

	/**
	 * Bind a ValidationAdvisorProvider. This default implementation returns a 3.4 advisor, configured with
	 * a DefaultPotentialProblemsAdvisor.
	 *
	 * @return
	 */
	public Provider<IValidationAdvisor> provideValidationAdvisor() {
		return ValidationAdvisorProvider.create(PuppetTarget.getDefault().getComplianceLevel());
	}

}
