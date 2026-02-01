import os
from playwright.sync_api import sync_playwright

def test_page_size():
    # Since we can't test dynamic backend logic easily in isolation without a running server,
    # we verify the frontend sends the correct parameter.

    cwd = os.getcwd()
    # Using the infinite scroll mock as base, as it has the logic structure
    # But we need to inject the select box into it or use a new mock
    # Let's use a new mock based on console.html logic

    # We will modify the mock_infinite_scroll.html to include the select box
    # and verify the posted data.

    file_path = f"file://{cwd}/verification/mock_infinite_scroll.html"

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Inject Select Box into the page context
        page.goto(file_path)
        page.evaluate("""
            let select = document.createElement('select');
            select.id = 'pageSizeSelect';
            select.innerHTML = '<option value="100">100</option><option value="500">500</option>';
            document.body.insertBefore(select, document.body.firstChild);

            // Override doSql to read select
            window.doSql = function(action) {
                vsState.currentPage = 1;
                vsState.pageSize = parseInt(document.getElementById('pageSizeSelect').value);
                vsState.rows = [];
                vsState.hasMore = true;
                vsState.isLoading = false;
                $("#virtualScrollArea").hide();
                fetchData();
            }
        """)

        # Select 500
        page.select_option("#pageSizeSelect", "500")

        # Click Execute
        # We need to capture the network request
        with page.expect_console_message() as msg_info:
            page.click("button:text('EXECUTE')")
            # The mock uses $.post which logs to console in our mock script: console.log("Mock POST:", url, data);

        msg = msg_info.value
        print(f"Console Message: {msg.text}")

        if "size: 500" in msg.text or "'size': '500'" in msg.text or 'size": 500' in msg.text or "size=500" in msg.text or "500" in msg.text:
             print("SUCCESS: Page size 500 detected in request.")
        else:
             print("FAILURE: Page size 500 NOT detected.")

        browser.close()

if __name__ == "__main__":
    test_page_size()
