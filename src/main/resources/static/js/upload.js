// 파일 업로드
async function uploadToServer(formObj) {
    console.log("upload to server...");
    const response = await axios({
        method: 'post',
        url: '/upload',
        data: formObj,
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });

    return response.data;
}

// S3 파일 삭제 (기존 deleteFile -> removeFileToServer 명칭 정합)
async function removeFileToServer(fileName) {
    const response = await axios.delete(`/remove/${fileName}`);
    return response.data;
}