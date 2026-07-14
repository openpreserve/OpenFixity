import { useEffect } from 'react';

export default function JobsPage() {
  useEffect(() => {
    document.title = 'Jobs | OpenFixity';
  }, []);

  return (
    <div>
      <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-6">Jobs</h2>
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <p className="text-gray-600 dark:text-gray-300">Jobs page coming soon...</p>
      </div>
    </div>
  )
}
