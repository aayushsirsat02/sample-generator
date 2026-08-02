
/*
 * USER / ADMIN SWITCH
 */

function changeLoginType(type) {

    const userButton =
        document.getElementById("userButton");

    const adminButton =
        document.getElementById("adminButton");

    const loginTitle =
        document.getElementById("loginTitle");

    const loginDescription =
        document.getElementById("loginDescription");

    const loginButton =
        document.getElementById("loginButton");

    const loginType =
        document.getElementById("loginType");


    if (type === "user") {

        userButton.classList.add("active");

        adminButton.classList.remove("active");

        loginTitle.textContent =
            "Welcome back";

        loginDescription.textContent =
            "Enter your credentials to access your dashboard.";

        loginButton.textContent =
            "Login";

        loginType.value =
            "user";

    } else {

        adminButton.classList.add("active");

        userButton.classList.remove("active");

        loginTitle.textContent =
            "Admin";

        loginDescription.textContent =
            "Enter your administrator credentials.";

        loginButton.textContent =
            "Login as Admin";

        loginType.value =
            "admin";

    }
}


/*
 * PASSWORD SHOW / HIDE
 */

function togglePassword() {

    const password =
        document.getElementById("password");

    const toggle =
        document.getElementById("passwordToggle");


    if (password.type === "password") {

        password.type = "text";

        toggle.textContent = "Hide";

    } else {

        password.type = "password";

        toggle.textContent = "Show";

    }
}


/*
 * FORM SUBMIT
 */

document.addEventListener("DOMContentLoaded", function () {

    const loginForm =
        document.getElementById("loginForm");

    const loginButton =
        document.getElementById("loginButton");


    loginForm.addEventListener("submit", function () {

        loginButton.disabled = true;

        loginButton.textContent = "Logging in...";

    });

});

