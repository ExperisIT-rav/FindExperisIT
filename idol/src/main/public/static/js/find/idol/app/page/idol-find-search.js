/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'find/app/page/find-search',
    'underscore',
    'i18n!find/nls/bundle',
    'find/idol/nls/root/snapshots',
    'find/app/model/saved-searches/saved-search-model',
    'find/idol/app/page/search/idol-service-view',
    'find/idol/app/page/search/suggest/idol-suggest-view',
    'find/idol/app/page/search/snapshots/snapshot-data-view',
    'find/idol/app/page/search/comparison/comparison-view',
    'find/app/page/search/results/state-token-strategy',
    'find/idol/app/model/comparison/comparison-documents-collection',
    'find/app/page/search/related-concepts/related-concepts-click-handlers',
    'find/idol/app/page/search/idol-query-left-side-view',
    'find/idol/app/page/search/comparison/compare-modal',
    'find/app/configuration'
], function(FindSearch, _, i18n, snapshotsI18n, SavedSearchModel, ServiceView, SuggestView, SnapshotDataView, ComparisonView, stateTokenStrategy,
            ComparisonDocumentsCollection, relatedConceptsClickHandlers, IdolQueryLeftSideView, CompareModal, configuration) {

    'use strict';

    return FindSearch.extend({
        ServiceView: ServiceView,
        SuggestView: SuggestView,
        QueryLeftSideView: IdolQueryLeftSideView,

        getSearchTypes: function() {
            var searchTypes = FindSearch.prototype.getSearchTypes.call(this);

            if (configuration().hasBiRole) {
                searchTypes = _.extend({
                    SNAPSHOT: {
                        cssClass: 'snapshot',
                        autoCorrect: false,
                        queryTextModelChange: _.constant(_.noop),
                        collection: 'savedSnapshotCollection',
                        icon: 'hp-camera',
                        isMutable: false,
                        fetchStrategy: stateTokenStrategy,
                        showTimeBar: false,
                        DocumentsCollection: ComparisonDocumentsCollection,
                        LeftSideFooterView: SnapshotDataView,
                        MiddleColumnHeaderView: null,
                        relatedConceptsClickHandler: relatedConceptsClickHandlers.newQuery,
                        openEditText: {
                            create: snapshotsI18n['openEdit.create'],
                            edit: snapshotsI18n['openEdit.edit']
                        },
                        createSearchModelAttributes: function() {
                            return {inputText: '', relatedConcepts: []};
                        },
                        searchModelChange: function(options) {
                            return function() {
                                var newSearch = new SavedSearchModel({
                                    queryText: options.searchModel.get('inputText'),
                                    relatedConcepts: [],
                                    title: i18n['search.newSearch'],
                                    type: SavedSearchModel.Type.QUERY
                                });

                                options.savedQueryCollection.add(newSearch);
                                options.selectedTabModel.set('selectedSearchCid', newSearch.cid);
                            };
                        }
                    }
                }, searchTypes);
            }

            return searchTypes;
        },

        serviceViewOptions: function(cid) {
            return {
                comparisonModalCallback: function() {
                    new CompareModal({
                        cid: cid,
                        savedSearchCollection: this.savedSearchCollection,
                        queryStates: this.queryStates,
                        comparisonSuccessCallback: function(model, searchModels) {
                            this.removeComparisonView();

                            this.$('.service-view-container').addClass('hide');
                            this.$('.comparison-service-view-container').removeClass('hide');

                            this.comparisonView = new ComparisonView({
                                model: model,
                                searchModels: searchModels,
                                scrollModel: this.windowScrollModel,
                                escapeCallback: function() {
                                    this.removeComparisonView();
                                    this.$('.service-view-container').addClass('hide');
                                    this.$('.query-service-view-container').removeClass('hide');
                                }.bind(this)
                            });
                            this.comparisonView.$el.insertBefore(this.$('.hp-logo-footer'));
                            this.comparisonView.render();
                        }.bind(this)
                    });
                }.bind(this)
            };
        },

        documentDetailOptions: function (database, reference) {
            return {
                reference: reference,
                database: database
            };
        },

        suggestOptions: function (database, reference) {
            return {
                database: database,
                reference: reference
            };
        },

        removeComparisonView: function() {
            if (this.comparisonView) {
                // Setting the element to nothing prevents the containing element from being removed when the view is removed
                this.comparisonView.setElement();
                this.comparisonView.remove();
                this.stopListening(this.comparisonView);
                this.comparisonView = null;
            }
        },

        remove: function() {
            this.removeComparisonView();
            FindSearch.prototype.remove.call(this);
        }
    });
});
