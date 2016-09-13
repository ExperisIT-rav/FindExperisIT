/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'find/app/page/search/filters/indexes/indexes-view',
    'js-whatever/js/escape-hod-identifier',
    'i18n!find/nls/indexes'
], function (IndexesView, escapeHodIdentifier, i18n) {
    'use strict';

    return IndexesView.extend({
        getIndexCategories: function () {
            return [
                {
                    name: 'private',
                    displayName: i18n['search.indexes.privateIndexes'],
                    className: 'list-unstyled',
                    filter: function(model) {
                    return model.get('domain') !== 'PUBLIC_INDEXES';
                }
            }, {
                    name: 'public',
                    displayName: i18n['search.indexes.publicIndexes'],
                    className: 'list-unstyled',
                    filter: function(model) {
                        return model.get('domain') === 'PUBLIC_INDEXES';
                    }
                }
            ];
        }
    });

});
