function scanPath(collectionName, pathId) {
    console.log(`Scanning path ${pathId} in collection ${collectionName}`);
    $.post(`/api/collections/${collectionName}/paths/${pathId}/scan`, function() {
        $('#scanModal').modal('show');
    });
}
function closeScanModal() {
    location.reload();
}
function closeRegistrationModal() {
    location.reload();
}
function deregisterPath(collectionName, pathId) {
    console.log(`Deresitering path ${pathId} in collection ${collectionName}`);
    $.ajax({
            url: `/api/collections/${collectionName}/paths/${pathId}`,
            type: 'DELETE',
            success: function() {
                $('#registrationModal').modal('show');
            }
    });
}
