import { useAppInfo } from '@/hooks/api'

export default function Footer() {
  const { data: appInfo } = useAppInfo()

  const frontendVersion = __APP_VERSION__
  const frontendBuildTime = __BUILD_TIME__
  const backendVersion = appInfo?.version ?? 'unknown'
  const javaVersion = appInfo?.javaVersion ?? 'unknown'
  const runtime = appInfo ? `${appInfo.osName}/${appInfo.osArch}` : 'unknown'

  return (
    <footer className="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 py-4 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row justify-between items-center text-sm text-gray-600 dark:text-gray-400">
          <div className="mb-2 sm:mb-0">
            &copy; {new Date().getFullYear()} Open Preservation Foundation
          </div>
          <div className="flex gap-4 text-xs">
            <span>Frontend: {frontendVersion}</span>
            <span>Backend: {backendVersion}</span>
            <span>Java: {javaVersion}</span>
            <span>Runtime: {runtime}</span>
            <span>Build: {frontendBuildTime}</span>
          </div>
        </div>
      </div>
    </footer>
  )
}
