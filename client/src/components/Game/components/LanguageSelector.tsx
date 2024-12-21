import {
  Button,
  Menu,
  MenuButton,
  MenuItem,
  MenuList,
  Text,
} from "@chakra-ui/react";
import { LANGUAGE_VERSIONS, Language } from "../constants";

const languages = Object.entries(LANGUAGE_VERSIONS);
const ACTIVE_COLOR = "blue.400";

type LanguageSelectorProps = {
  language: Language;
  onSelect: (language: Language) => void;
};

// dropdown menu for selecting langauge
export default function LanguageSelector({
  language,
  onSelect,
}: LanguageSelectorProps) {
  return (
    <div className="pl-2 pb-0.5">
      <Menu isLazy>
        <MenuButton as={Button} size="sm">
          {language}
        </MenuButton>
        <MenuList bg="#110c1b">
          {languages.map(([lang, version]) => (
            <MenuItem
              key={lang as Language}
              color={
                (lang as Language) === (language as Language)
                  ? ACTIVE_COLOR
                  : ""
              }
              bg={
                (lang as Language) === (language as Language)
                  ? "gray.900"
                  : "transparent"
              }
              _hover={{
                color: ACTIVE_COLOR,
                bg: "gray.900",
              }}
              onClick={() => onSelect(lang as Language)}
            >
              {lang}
              &nbsp;
              <Text as="span" color="gray.600" fontSize="sm">
                ({version})
              </Text>
            </MenuItem>
          ))}
        </MenuList>
      </Menu>
    </div>
  );
}
