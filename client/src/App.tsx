import { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { ClerkProvider, useAuth } from "@clerk/clerk-react";
import { ReactNotifications } from "react-notifications-component";
import Home from "./components/Home/Home";
import WaitingRoom from "./components/Waiting/WaitingRoom";
import GamePage from "./components/Game/GamePage";
import ProfilePage from "./components/Profile/ProfilePage";
import BinaryBackground from "./components/Home/BinaryBackground";
import WaitingForGame from "./components/Waiting/WaitingForGame";
import Countdown from "./components/Waiting/Countdown";
import FormPage from "./Form";
import "./index.css";

// loading animation
export function Loading() {
  return (
    <div className="flex items-center justify-center w-screen h-screen bg-custom-radial from-green-800 to-green-950 text-white">
      <BinaryBackground />
      <img
        src="./loading5.gif"
        alt="loading..."
        width="5%"
        height="5%"
        className="opacity-65"
      />
    </div>
  );
}

// set up the routes, if app not ready then return loading screen
function AppRoutes() {
  const { isLoaded } = useAuth();
  const [appReady, setAppReady] = useState(false);

  useEffect(() => {
    if (isLoaded) {
      const timeout = setTimeout(() => setAppReady(true), 200);
      return () => clearTimeout(timeout);
    } else {
      setAppReady(false);
    }
  }, [isLoaded]);

  if (!appReady) {
    return <Loading />;
  }

  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/lobby" element={<WaitingRoom />} />
        <Route path="/game" element={<GamePage />} />
        <Route path="/pregame" element={<WaitingForGame />} />
        <Route path="/profile" element={<ProfilePage />}></Route>
        <Route path="/countdown" element={<Countdown />}></Route>
        <Route path="/form" element={<FormPage />}></Route>
      </Routes>
    </Router>
  );
}

const PUBLISHABLE_KEY = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY;

if (!PUBLISHABLE_KEY) {
  throw new Error("Missing Clerk Publishable Key");
}

// set up app with clerk
function App() {
  return (
    <div className="flex w-screen h-screen overflow-hidden bg-custom-radial from-green-800 to-green-950">
      <ClerkProvider publishableKey={PUBLISHABLE_KEY} afterSignOutUrl="/">
        <ReactNotifications></ReactNotifications>
        <AppRoutes />
      </ClerkProvider>
    </div>
  );
}

export default App;
