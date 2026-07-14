import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import CollectionsPage from './pages/CollectionsPage'
import CollectionDetailPage from './pages/CollectionDetailPage'
import PathsPage from './pages/PathsPage'
import PathDetailPage from './pages/PathDetailPage'
import ScansPage from './pages/ScansPage'
import ScanDetailPage from './pages/ScanDetailPage'
import DashboardPage from './pages/DashboardPage'
import SettingsPage from './pages/SettingsPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="collections" element={<CollectionsPage />} />
        <Route path="collections/:name" element={<CollectionDetailPage />} />
        <Route path="paths" element={<PathsPage />} />
        <Route path="paths/:id" element={<PathDetailPage />} />
        <Route path="scans" element={<ScansPage />} />
        <Route path="scans/:id" element={<ScanDetailPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  )
}

export default App
