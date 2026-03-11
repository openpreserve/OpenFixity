function scanCollection(collectionName, pathId) {
    console.log(`Scanning path ${pathId} in collection ${collectionName}`);
    $.post(`/api/collections/${collectionName}/paths/${pathId}/scan`, function() {
        $('#scanModal').modal('show');
    });
}
function scanPath(pathId) {
    $('#algorithmsModal .btn-primary').off('click').on('click', function() {
        const algorithm = $('#algorithmsRadioGroup input[name="algorithmsRadio"]:checked').val();
        console.log(`Scanning path ${pathId} with algorithm ${algorithm}`);
        $('#algorithmsModal').modal('hide');
        $.post(`/api/paths/${pathId}/scan/${algorithm}`, function() {
            location.reload();
        });
    });
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
