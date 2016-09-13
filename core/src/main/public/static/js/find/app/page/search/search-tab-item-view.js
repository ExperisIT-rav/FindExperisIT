/*
 * Copyright 2016 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'js-whatever/js/list-item-view',
    'underscore',
    'i18n!find/nls/bundle',
    'find/app/model/saved-searches/saved-search-model',
    'text!find/templates/app/page/search/search-tab-item-view.html'
], function(ListItemView, _, i18n, SavedSearchModel, template) {

    'use strict';

    var templateFunction = _.template(template);
    var NEW_DOCS_LIMIT = 1000;

    return ListItemView.extend({
        className: 'search-tab',
        tagName: 'li',
        queryState: null,

        initialize: function(options) {
            var cid = this.model.cid;
            this.queryStates = options.queryStates;

            ListItemView.prototype.initialize.call(this, _.defaults({
                template: templateFunction,
                templateOptions: {
                    i18n: i18n,
                    searchCid: cid,
                    icon: options.searchTypes[this.model.get('type')].icon
                }
            }, options));

            this.listenTo(this.model, 'change', this.updateSavedness);
            this.listenTo(this.model, 'change:newDocuments', this.updateTabBadge);
            this.listenTo(this.queryStates, 'change:' + cid, function() {
                this.updateQueryStateListeners();
                this.updateSavedness();
            });

            this.updateQueryStateListeners();
        },

        render: function() {
            ListItemView.prototype.render.apply(this, arguments);

            this.updateSavedness();
            this.updateTabBadge();
        },

        updateTabBadge: function() {
            var newDocuments = this.model.get('newDocuments');

            if(newDocuments > 0) {
                this.$('.new-document-label')
                    .removeClass('hide')
                    .text(newDocuments > NEW_DOCS_LIMIT ? NEW_DOCS_LIMIT + '+' : newDocuments);
            } else {
                this.$('.new-document-label')
                    .addClass('hide');
            }
        },

        updateSavedness: function() {
            var changed = this.queryState ? !this.model.equalsQueryState(this.queryState) : false;
            this.$('.search-tab-anchor').toggleClass('bold', this.model.isNew() || changed);
            this.$('.search-tab-anchor .hp-new').toggleClass('hide', !this.model.isNew() && !changed);
        },

        updateQueryStateListeners: function() {
            var newQueryState = this.queryStates.get(this.model.cid);

            if (this.queryState) {
                this.stopListening(this.queryState.selectedIndexes);
                this.stopListening(this.queryState.queryTextModel);
                this.stopListening(this.queryState.selectedParametricValues);
                this.stopListening(this.queryState.datesFilterModel);
            }

            this.queryState = newQueryState;

            if (this.queryState) {
                this.listenTo(this.queryState.selectedIndexes, 'add remove', this.updateSavedness);
                this.listenTo(this.queryState.queryTextModel, 'change', this.updateSavedness);
                this.listenTo(this.queryState.selectedParametricValues, 'add remove', this.updateSavedness);
                this.listenTo(this.queryState.datesFilterModel, 'change', this.updateSavedness);
            }
        }
    });

});
