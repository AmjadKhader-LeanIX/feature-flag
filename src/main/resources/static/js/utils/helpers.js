export function formatDate(dateString) {
    return new Date(dateString).toLocaleString();
}

export function formatJsonDiff(oldValues, newValues) {
    const changes = [];
    if (!oldValues && newValues) {
        Object.entries(newValues).forEach(([key, value]) => {
            changes.push({ field: key, old: null, new: value, changed: true });
        });
    } else if (oldValues && !newValues) {
        Object.entries(oldValues).forEach(([key, value]) => {
            changes.push({ field: key, old: value, new: null, changed: true });
        });
    } else if (oldValues && newValues) {
        const allKeys = new Set([...Object.keys(oldValues), ...Object.keys(newValues)]);
        allKeys.forEach(key => {
            const oldVal = oldValues[key];
            const newVal = newValues[key];
            if (JSON.stringify(oldVal) !== JSON.stringify(newVal)) {
                changes.push({
                    field: key,
                    old: oldVal !== undefined ? oldVal : null,
                    new: newVal !== undefined ? newVal : null,
                    changed: true
                });
            }
        });
    }
    return changes;
}

export function showToast(toast, message, type = 'info') {
    toast.message = message;
    toast.type = type;
    toast.visible = true;
    setTimeout(() => {
        toast.visible = false;
    }, 5000);
}
