import { ChakraProvider } from "@chakra-ui/react";
import theme from "./theme.ts";
import CodeBox from "./CodeBox.tsx";
import { GlobalStateProvider } from "./GlobalStateProvider.tsx";
import { useLocation } from "react-router-dom";

// wrapper for game logic and components
export default function GamePage() {
  const location = useLocation();
  const { roomID } = location.state || {};

  return (
    <GlobalStateProvider>
      <ChakraProvider theme={theme}>
        <CodeBox roomID={roomID} />
      </ChakraProvider>
    </GlobalStateProvider>
  );
}
