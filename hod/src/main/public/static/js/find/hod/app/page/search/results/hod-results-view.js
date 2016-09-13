/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'find/app/page/search/results/results-view',
    'i18n!find/nls/errors'
], function (ResultsView, i18n) {
    'use strict';

    return ResultsView.extend({
        generateErrorMessage: function (xhr) {
            var message = i18n['error.default.message'];

            if (xhr.responseJSON) {
                if (xhr.responseJSON.hodErrorCode && i18n["hod.error." + xhr.responseJSON.hodErrorCode]) {
                    message = i18n["hod.error." + xhr.responseJSON.hodErrorCode];
                } else if (xhr.responseJSON.uuid) {
                    message = i18n['error.default.message.uuid'](xhr.responseJSON.uuid);
                }
            }

            return message;
        },

        generateSuggestRoute : function (resultNode) {
            var domain = encodeURIComponent(resultNode.attr('data-domain'));
            var index = encodeURIComponent(resultNode.attr('data-index'));
            var reference = encodeURIComponent(resultNode.attr('data-reference'));
            return 'find/search/suggest/' + domain + '/' + index + '/' + reference;
        }
    });
});
