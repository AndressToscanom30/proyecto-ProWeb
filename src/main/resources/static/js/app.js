(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {

        initStatusForms();
        initOverrideDateToggle();
        initConfirmDialogs();
        initTableSearch();
    });

    function initStatusForms() {
        document.querySelectorAll('.status-form select').forEach(function(select) {
            select.addEventListener('change', function() {
                this.closest('form').submit();
            });
        });
    }

    function initOverrideDateToggle() {
        var overrideInput = document.querySelector('input[name="dueDateOverride"]');
        var reasonGroup = document.querySelector('.override-reason');
        if (overrideInput && reasonGroup) {
            overrideInput.addEventListener('change', function() {
                reasonGroup.style.display = this.value ? 'block' : 'none';
            });
            if (overrideInput.value) {
                reasonGroup.style.display = 'block';
            }
        }
    }

    function initConfirmDialogs() {
        document.querySelectorAll('[data-confirm]').forEach(function(el) {
            el.addEventListener('click', function(e) {
                if (!confirm(this.getAttribute('data-confirm'))) {
                    e.preventDefault();
                }
            });
        });
    }

    function initTableSearch() {
        var searchInput = document.getElementById('tableSearch');
        if (!searchInput) return;

        searchInput.addEventListener('input', function() {
            var query = this.value.toLowerCase();
            var table = document.querySelector('.data-table');
            if (!table) return;

            table.querySelectorAll('tbody tr').forEach(function(row) {
                var text = row.textContent.toLowerCase();
                row.style.display = text.includes(query) ? '' : 'none';
            });
        });
    }

})();
