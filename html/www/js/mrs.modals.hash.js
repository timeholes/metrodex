/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 * Copyright © 2016-2017 Jelurida IP B.V.                                     *
 * Copyright © 2018 metro.software                                            *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of the Nxt software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {mrs.js}
 * @depends {mrs.modals.js}
 */
var MRS = (function(MRS, $, undefined) {
	$("#hash_modal").on("show.bs.modal", function(e) {
		$("#hash_calculation_output").html("").hide();
		$("#hash_modal_button").data("form", "calculate_hash_form");
	});

	MRS.forms.hash = function($modal) {
		var data = $.trim($("#calculate_hash_data").val());
		if (!data) {
			$("#hash_calculation_output").html("").hide();
			return {
				"error": "Data is a required field."
			};
		} else {
			return {};
		}
	};

	MRS.forms.hashComplete = function(response, data) {
		$("#hash_modal").find(".error_message").hide();

		if (response.hash) {
			$("#hash_calculation_output").html($.t("calculated_hash_is") + "<br/><br/>" +
				"<textarea style='width:100%' rows='3'>" + MRS.escapeRespStr(response.hash) + "</textarea>").show();
		} else {
			$.growl($.t("error_calculate_hash"), {
				"type": "danger"
			});
		}
	};

	MRS.forms.hashError = function() {
		$("#hash_calculation_output").hide();
	};

	return MRS;
}(MRS || {}, jQuery));