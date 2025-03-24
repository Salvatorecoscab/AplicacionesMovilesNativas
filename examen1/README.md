<!-- Desarrolle una aplicación móvil nativa en Android que sirva como herramienta educativa para enseñar a niños
de primaria sobre la representación binaria de información. La aplicación debe ser interactiva, visualmente
atractiva y adaptada al nivel de comprensión de este público objetivo. -->
Se desarrolló una aplicación móvil nativa en Android que sirve como herramienta educativa para enseñar a niños
de primaria sobre la representación binaria de información. La aplicación es interactiva, visualmente
atractiva y adaptada al nivel de comprensión de este público objetivo.

<!-- Implementación de temas personalizables:
○ La aplicación debe implementar dos temas distintos que el usuario pueda seleccionar:
■ Tema Guinda (color representativo del IPN)
■ Tema Azul (color representativo de la ESCOM)
○ Ambos temas deben responder automáticamente al modo del sistema:
■ Versión clara cuando el dispositivo esté en modo claro
■ Versión oscura cuando el dispositivo esté en modo oscuro

2. Arquitectura de la aplicación:
○ Implementar navegación entre al menos 3 Activities diferentes
○ Utilizar mínimo 2 Fragments para mostrar diferentes secciones de contenido
○ Incluir un menú o sección que permita al usuario cambiar entre los temas disponibles
3. Interfaz de usuario:
○ Diseño responsivo que se adapte a diferentes tamaños de pantalla
○ Elementos interactivos apropiados para niños (botones grandes, colores llamativos,
animaciones)
○ Navegación clara e intuitiva -->
Se implementaron dos temas distintos que el usuario puede seleccionar>
- Tema Guinda (color representativo del IPN)
- Tema Azul (color representativo de la ESCOM)
Estos temas son solo para el menú de la aplicación, no afectan a la interfaz de las actividades, ni los fragments.
Además se cambia el modo a claro, cuando el dispositivo esté en modo claro y a oscuro cuando el dispositivo esté en modo oscuro.

Se implementó la navegación entre 3 Activities diferentes:
- MainActivity, el cual es el menú de la aplicación, que me permite seleccionar el tema de la aplicación y la actividad que se desea abrir.
- BinaryActivity, el cual es la actividad principal de la aplicación, en la cual se muestra la representación binaria de un número decimal.
- UnitsActivity, que nos permite traducir una palabra en ASCII a texto para poder visualizar la representación binaria de la palabra.

Se implementó una interfaz de usuario que se adapta a diferentes tamaños de pantalla, con elementos interactivos apropiados para niños. Con botones grandes y una estructura simple y clara.
Además tiene una navegación clara e intuitiva.

# Juego 1
El juego se basa en interruptores para poder transformar un numero decimal a binario, al finalizar se muestra si el resultado es correcto o erróneo en un fragment y se permite al usuario salir del juego o volver a jugar

# Juego 2
El juego consiste en adivinar una palabra que está escrita en ASCII, para esto el usuario puede revisar unas pistas en la parte superior derecha, al igual que la actividad anterior, se tiene un fragment para mostrar si el resultado es correcto o erróneo y jugar de nuevo o no