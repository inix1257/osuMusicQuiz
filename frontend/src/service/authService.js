import axios from "axios";
import apiService from "../api/apiService";

export async function getUserData(token) {
    return apiService.get(`${process.env.VUE_APP_API_URL}/api/getme`, {
        params: {
            token: token
        }
    }).then((response) => {
        return response.data;
    }).catch(async (error) => {
        if (error.response.status === 401) {
            return null;
        }
        if (error.response.status === 403) {
            alert("You have been banned from OMQ.")
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            location.reload();
            return null;
        } else if (error.response.status === 406) {
            alert("Your account should be older than a month to play OMQ.")
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            location.reload();
            return null;
        }
        throw error;
    });
}